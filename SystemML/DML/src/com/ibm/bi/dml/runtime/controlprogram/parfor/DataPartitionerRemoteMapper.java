package com.ibm.bi.dml.runtime.controlprogram.parfor;

import java.io.IOException;
import java.util.StringTokenizer;

import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.Mapper;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reporter;

import com.ibm.bi.dml.runtime.controlprogram.ParForProgramBlock.PDataPartitionFormat;
import com.ibm.bi.dml.runtime.controlprogram.parfor.util.PairWritableBlock;
import com.ibm.bi.dml.runtime.controlprogram.parfor.util.PairWritableCell;
import com.ibm.bi.dml.runtime.matrix.io.InputInfo;
import com.ibm.bi.dml.runtime.matrix.io.MatrixBlock;
import com.ibm.bi.dml.runtime.matrix.io.MatrixCell;
import com.ibm.bi.dml.runtime.matrix.io.MatrixIndexes;
import com.ibm.bi.dml.runtime.matrix.mapred.MRJobConfiguration;

/**
 * Remote data partitioner mapper implementation that does the actual
 * partitioning and key creation according to the given format.
 *
 */
public class DataPartitionerRemoteMapper 
	implements Mapper<Writable, Writable, Writable, Writable>
{	
	private DataPartitionerMapper _mapper = null;
	
	public DataPartitionerRemoteMapper( ) 
	{
		
	}
	
	/**
	 * 
	 */
	public void map(Writable key, Writable value, OutputCollector<Writable, Writable> out, Reporter reporter) 
		throws IOException
	{
		_mapper.processKeyValue(key, value, out, reporter);		
	}

	/**
	 * 
	 */
	public void configure(JobConf job)
	{
		long rlen = MRJobConfiguration.getPartitioningNumRows( job );
		long clen = MRJobConfiguration.getPartitioningNumCols( job );
		int brlen = MRJobConfiguration.getPartitioningBlockNumRows( job );
		int bclen = MRJobConfiguration.getPartitioningBlockNumCols( job );
		InputInfo ii = MRJobConfiguration.getPartitioningInputInfo( job );
		PDataPartitionFormat pdf = MRJobConfiguration.getPartitioningFormat( job );
		
		if( ii == InputInfo.TextCellInputInfo )
			_mapper = new DataPartitionerMapperTextcell(rlen, clen, brlen, bclen, pdf);
		else if( ii == InputInfo.BinaryCellInputInfo )
			_mapper = new DataPartitionerMapperBinarycell(rlen, clen, brlen, bclen, pdf);
		else if( ii == InputInfo.BinaryBlockInputInfo )
			_mapper = new DataPartitionerMapperBinaryblock(rlen, clen, brlen, bclen, pdf);
		else
			throw new RuntimeException("Unable to configure mapper with unknown input info: "+ii.toString());
	}
	
	/**
	 * 
	 */
	@Override
	public void close() throws IOException 
	{
		//do nothing
	}
	
	private abstract class DataPartitionerMapper //NOTE: could also be refactored as three different mappers
	{
		protected long _rlen = -1;
		protected long _clen = -1;
		protected int _brlen = -1;
		protected int _bclen = -1;
		protected PDataPartitionFormat _pdf = null;
	
		protected DataPartitionerMapper( long rlen, long clen, int brlen, int bclen, PDataPartitionFormat pdf )
		{
			_rlen = rlen;
			_clen = clen;
			_brlen = brlen;
			_bclen = bclen;
			_pdf = pdf;
		}
		
		protected abstract void processKeyValue( Writable key, Writable value, OutputCollector<Writable, Writable> out, Reporter reporter ) 
			throws IOException;
	}
	
	private class DataPartitionerMapperTextcell extends DataPartitionerMapper
	{
		protected DataPartitionerMapperTextcell(long rlen, long clen, int brlen, int bclen, PDataPartitionFormat pdf) 
		{
			super(rlen, clen, brlen, bclen, pdf);
		}

		@Override
		protected void processKeyValue(Writable key, Writable value, OutputCollector<Writable, Writable> out, Reporter reporter) 
			throws IOException 
		{
			long row = -1;
			long col = -1;
			
			try
			{
				String cellStr = ((Text)value).toString().trim();		
				StringTokenizer st = new StringTokenizer(cellStr, " ");
				row = Long.parseLong( st.nextToken() );
				col = Long.parseLong( st.nextToken() );
				double lvalue = Double.parseDouble( st.nextToken() );
				
				LongWritable longKey = new LongWritable();
				switch( _pdf )
				{
					case ROW_WISE:
						longKey.set( row );
						row = 1;
						break;
					case ROW_BLOCK_WISE:
						longKey.set( (row-1)/_brlen+1 );
						row = (row-1)%_brlen+1;
						break;
					case COLUMN_WISE:
						longKey.set( col );
						col = 1;
						break;
					case COLUMN_BLOCK_WISE:
						longKey.set( (col-1)/_bclen+1 );
						col = (col-1)%_bclen+1;
						break;
				}
				
				StringBuilder sb = new StringBuilder();
				sb.append(row);
				sb.append(" ");
				sb.append(col);
				sb.append(" ");
				sb.append(lvalue);
				Text outValue = new Text(sb.toString());
					
				out.collect(longKey, outValue);	
			} 
			catch (Exception e) 
			{
				//post-mortem error handling and bounds checking
				if( row < 1 || row > _rlen || col < 1 || col > _clen )
				{
					throw new IOException("Matrix cell ["+(row)+","+(col)+"] " +
										  "out of overall matrix range [1:"+_rlen+",1:"+_clen+"].");
				}
				else
					throw new IOException("Unable to partition text cell matrix.", e);
			}
		}	
	}
	
	private class DataPartitionerMapperBinarycell extends DataPartitionerMapper
	{
		protected DataPartitionerMapperBinarycell(long rlen, long clen, int brlen, int bclen, PDataPartitionFormat pdf) 
		{
			super(rlen, clen, brlen, bclen, pdf);
		}

		@Override
		protected void processKeyValue(Writable key, Writable value, OutputCollector<Writable, Writable> out, Reporter reporter) 
			throws IOException 
		{
			long row = -1;
			long col = -1;

			try
			{
				LongWritable longKey = new LongWritable();
				PairWritableCell pairValue = new PairWritableCell();
				MatrixIndexes key2 = (MatrixIndexes)key;
				MatrixCell value2 = (MatrixCell)value;
				row = key2.getRowIndex();
				col = key2.getColumnIndex();
				
				switch( _pdf )
				{
					case ROW_WISE:
						longKey.set(row);
						row = 1;
						break;
					case ROW_BLOCK_WISE:
						longKey.set( (row-1)/_brlen+1 );
						row = (row-1)%_brlen+1; 
						break;
					case COLUMN_WISE:
						longKey.set(col);
						col = 1;
						break;
					case COLUMN_BLOCK_WISE:
						longKey.set( (col-1)/_bclen+1 );
						col = (col-1)%_bclen+1; 
						break;
				}
				key2.setIndexes(row, col);	
				pairValue.indexes = key2;
				pairValue.cell = value2;
				out.collect(longKey, pairValue);
			} 
			catch (Exception e) 
			{
				//post-mortem error handling and bounds checking
				if( row < 1 || row > _rlen || col < 1 || col > _clen )
				{
					throw new IOException("Matrix cell ["+(row)+","+(col)+"] " +
										  "out of overall matrix range [1:"+_rlen+",1:"+_clen+"].");
				}
				else
					throw new IOException("Unable to partition binary cell matrix.", e);
			}
		}
	}
	
	private class DataPartitionerMapperBinaryblock extends DataPartitionerMapper
	{
		
		protected DataPartitionerMapperBinaryblock(long rlen, long clen, int brlen, int bclen, PDataPartitionFormat pdf) 
		{
			super(rlen, clen, brlen, bclen, pdf);
		}
		
		@Override
		protected void processKeyValue(Writable key, Writable value, OutputCollector<Writable, Writable> out, Reporter reporter) 
			throws IOException 
		{
			try
			{
				LongWritable longKey = new LongWritable();
				PairWritableBlock pairValue = new PairWritableBlock();
				MatrixIndexes key2 =  (MatrixIndexes)key;
				MatrixBlock value2 = (MatrixBlock)value;
				long row_offset = (key2.getRowIndex()-1)*_brlen;
				long col_offset = (key2.getColumnIndex()-1)*_bclen;
				
				boolean sparse = value2.isInSparseFormat();
				int rows = value2.getNumRows();
				int cols = value2.getNumColumns();
				
				//bound check per block
				if( row_offset + rows < 1 || row_offset + rows > _rlen || col_offset + cols<1 || col_offset + cols > _clen )
				{
					throw new IOException("Matrix block ["+(row_offset+1)+":"+(row_offset+rows)+","+(col_offset+1)+":"+(col_offset+cols)+"] " +
							              "out of overall matrix range [1:"+_rlen+",1:"+_clen+"].");
				}
				
				MatrixBlock tmp = null;				
				switch( _pdf )
				{
					case ROW_WISE:
						tmp = new MatrixBlock( 1, cols, false ); 
						tmp.spaceAllocForDenseUnsafe(1, cols);				
						for( int i=0; i<rows; i++ )
						{
							longKey.set(row_offset+1+i);
							key2.setIndexes(1, (col_offset/_bclen+1) );	
							if( sparse )
							{
								for( int j=0; j<cols; j++ )
								{
									double lvalue = value2.getValueSparseUnsafe(i, j);
									tmp.setValueDenseUnsafe(0, j, lvalue);
								}
							}
							else
							{
								for( int j=0; j<cols; j++ )
								{
									double lvalue = value2.getValueDenseUnsafe(i, j);
									tmp.setValueDenseUnsafe(0, j, lvalue);
								}
							}
							tmp.recomputeNonZeros();
							pairValue.indexes = key2;
							pairValue.block = tmp;
							out.collect(longKey, pairValue);
						}
						break;
					case ROW_BLOCK_WISE:
						longKey.set((row_offset/_brlen+1));
						key2.setIndexes(1, (col_offset/_bclen+1) );
						pairValue.indexes = key2;
						pairValue.block = value2;
						out.collect(longKey, pairValue);
						break;
					case COLUMN_WISE:
						tmp = new MatrixBlock( rows, 1, false ); 
						tmp.spaceAllocForDenseUnsafe(rows, 1);
						for( int i=0; i<cols; i++ )
						{
							longKey.set(col_offset+1+i);
							key2.setIndexes(row_offset/_brlen+1, 1);
							if( sparse )
							{
								for( int j=0; j<rows; j++ )
								{
									double lvalue = value2.getValueSparseUnsafe(j, i);
									tmp.setValueDenseUnsafe(j, 0, lvalue);
								}
							}
							else
							{
								for( int j=0; j<rows; j++ )
								{
									double lvalue = value2.getValueDenseUnsafe(j, i);
									tmp.setValueDenseUnsafe(j, 0, lvalue);
								}					
							}
							tmp.recomputeNonZeros();
							pairValue.indexes = key2;
							pairValue.block = tmp;
							out.collect(longKey, pairValue );
						}	
						break;
					case COLUMN_BLOCK_WISE:
						longKey.set(col_offset/_bclen+1);
						key2.setIndexes( row_offset/_brlen+1, 1 );
						pairValue.indexes = key2;
						pairValue.block = value2;
						out.collect(longKey, pairValue);
						break;
				}
			} 
			catch (Exception e) 
			{
				throw new IOException("Unable to partition binary block matrix.", e);
			}
		}
	}
}
