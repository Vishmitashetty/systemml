/**
 * (C) Copyright IBM Corp. 2010, 2015
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */

package com.ibm.bi.dml.runtime.instructions;

import java.util.HashMap;

import com.ibm.bi.dml.lops.Checkpoint;
import com.ibm.bi.dml.lops.DataGen;
import com.ibm.bi.dml.lops.WeightedSigmoid;
import com.ibm.bi.dml.lops.WeightedSigmoidR;
import com.ibm.bi.dml.lops.WeightedSquaredLoss;
import com.ibm.bi.dml.lops.WeightedSquaredLossR;
import com.ibm.bi.dml.runtime.DMLRuntimeException;
import com.ibm.bi.dml.runtime.DMLUnsupportedOperationException;
import com.ibm.bi.dml.runtime.instructions.spark.AggregateTernarySPInstruction;
import com.ibm.bi.dml.runtime.instructions.spark.AggregateUnarySPInstruction;
import com.ibm.bi.dml.runtime.instructions.spark.AppendGAlignedSPInstruction;
import com.ibm.bi.dml.runtime.instructions.spark.AppendGSPInstruction;
import com.ibm.bi.dml.runtime.instructions.spark.AppendMSPInstruction;
import com.ibm.bi.dml.runtime.instructions.spark.AppendRSPInstruction;
import com.ibm.bi.dml.runtime.instructions.spark.ArithmeticBinarySPInstruction;
import com.ibm.bi.dml.runtime.instructions.spark.BinUaggChainSPInstruction;
import com.ibm.bi.dml.runtime.instructions.spark.BuiltinBinarySPInstruction;
import com.ibm.bi.dml.runtime.instructions.spark.BuiltinUnarySPInstruction;
import com.ibm.bi.dml.runtime.instructions.spark.CSVReblockSPInstruction;
import com.ibm.bi.dml.runtime.instructions.spark.CentralMomentSPInstruction;
import com.ibm.bi.dml.runtime.instructions.spark.CheckpointSPInstruction;
import com.ibm.bi.dml.runtime.instructions.spark.CovarianceSPInstruction;
import com.ibm.bi.dml.runtime.instructions.spark.CpmmSPInstruction;
import com.ibm.bi.dml.runtime.instructions.spark.CumulativeAggregateSPInstruction;
import com.ibm.bi.dml.runtime.instructions.spark.CumulativeOffsetSPInstruction;
import com.ibm.bi.dml.runtime.instructions.spark.MapmmChainSPInstruction;
import com.ibm.bi.dml.runtime.instructions.spark.MapmmSPInstruction;
import com.ibm.bi.dml.runtime.instructions.spark.MatrixIndexingSPInstruction;
import com.ibm.bi.dml.runtime.instructions.spark.MatrixReshapeSPInstruction;
import com.ibm.bi.dml.runtime.instructions.spark.ParameterizedBuiltinSPInstruction;
import com.ibm.bi.dml.runtime.instructions.spark.PmmSPInstruction;
import com.ibm.bi.dml.runtime.instructions.spark.QuantilePickSPInstruction;
import com.ibm.bi.dml.runtime.instructions.spark.QuaternarySPInstruction;
import com.ibm.bi.dml.runtime.instructions.spark.RandSPInstruction;
import com.ibm.bi.dml.runtime.instructions.spark.ReblockSPInstruction;
import com.ibm.bi.dml.runtime.instructions.spark.RelationalBinarySPInstruction;
import com.ibm.bi.dml.runtime.instructions.spark.ReorgSPInstruction;
import com.ibm.bi.dml.runtime.instructions.spark.RmmSPInstruction;
import com.ibm.bi.dml.runtime.instructions.spark.SPInstruction;
import com.ibm.bi.dml.runtime.instructions.spark.SPInstruction.SPINSTRUCTION_TYPE;
import com.ibm.bi.dml.runtime.instructions.spark.TernarySPInstruction;
import com.ibm.bi.dml.runtime.instructions.spark.TsmmSPInstruction;
import com.ibm.bi.dml.runtime.instructions.spark.QuantileSortSPInstruction;
import com.ibm.bi.dml.runtime.instructions.spark.UaggOuterChainSPInstruction;
import com.ibm.bi.dml.runtime.instructions.spark.WriteSPInstruction;
import com.ibm.bi.dml.runtime.instructions.spark.ZipmmSPInstruction;


public class SPInstructionParser extends InstructionParser {
	
	public static final HashMap<String, SPINSTRUCTION_TYPE> String2SPInstructionType;
	static {
		String2SPInstructionType = new HashMap<String, SPInstruction.SPINSTRUCTION_TYPE>();
		
		//unary aggregate operators
		String2SPInstructionType.put( "uak+"   	, SPINSTRUCTION_TYPE.AggregateUnary);
		String2SPInstructionType.put( "uark+"   , SPINSTRUCTION_TYPE.AggregateUnary);
		String2SPInstructionType.put( "uack+"   , SPINSTRUCTION_TYPE.AggregateUnary);
		String2SPInstructionType.put( "uamean"  , SPINSTRUCTION_TYPE.AggregateUnary);
		String2SPInstructionType.put( "uarmean" , SPINSTRUCTION_TYPE.AggregateUnary);
		String2SPInstructionType.put( "uacmean" , SPINSTRUCTION_TYPE.AggregateUnary);
		String2SPInstructionType.put( "uamax"   , SPINSTRUCTION_TYPE.AggregateUnary);
		String2SPInstructionType.put( "uarmax"  , SPINSTRUCTION_TYPE.AggregateUnary);
		String2SPInstructionType.put( "uarimax",  SPINSTRUCTION_TYPE.AggregateUnary);
		String2SPInstructionType.put( "uacmax"  , SPINSTRUCTION_TYPE.AggregateUnary);
		String2SPInstructionType.put( "uamin"   , SPINSTRUCTION_TYPE.AggregateUnary);
		String2SPInstructionType.put( "uarmin"  , SPINSTRUCTION_TYPE.AggregateUnary);
		String2SPInstructionType.put( "uarimin" , SPINSTRUCTION_TYPE.AggregateUnary);
		String2SPInstructionType.put( "uacmin"  , SPINSTRUCTION_TYPE.AggregateUnary);
		String2SPInstructionType.put( "ua+"     , SPINSTRUCTION_TYPE.AggregateUnary);
		String2SPInstructionType.put( "uar+"    , SPINSTRUCTION_TYPE.AggregateUnary);
		String2SPInstructionType.put( "uac+"    , SPINSTRUCTION_TYPE.AggregateUnary);
		String2SPInstructionType.put( "ua*"     , SPINSTRUCTION_TYPE.AggregateUnary);
		String2SPInstructionType.put( "uatrace" , SPINSTRUCTION_TYPE.AggregateUnary);
		String2SPInstructionType.put( "uaktrace", SPINSTRUCTION_TYPE.AggregateUnary);

		//binary aggregate operators (matrix multiplication operators)
		String2SPInstructionType.put( "mapmm"      , SPINSTRUCTION_TYPE.MAPMM);
		String2SPInstructionType.put( "mapmmchain" , SPINSTRUCTION_TYPE.MAPMMCHAIN);
		String2SPInstructionType.put( "tsmm"       , SPINSTRUCTION_TYPE.TSMM);
		String2SPInstructionType.put( "cpmm"   	   , SPINSTRUCTION_TYPE.CPMM);
		String2SPInstructionType.put( "rmm"        , SPINSTRUCTION_TYPE.RMM);
		String2SPInstructionType.put( "pmm"        , SPINSTRUCTION_TYPE.PMM);
		String2SPInstructionType.put( "zipmm"      , SPINSTRUCTION_TYPE.ZIPMM);
		
		String2SPInstructionType.put( "uaggouterchain", SPINSTRUCTION_TYPE.UaggOuterChain);
		
		//ternary aggregate operators
		String2SPInstructionType.put( "tak+*"      , SPINSTRUCTION_TYPE.AggregateTernary);

		
		String2SPInstructionType.put( "rangeReIndex"   	, SPINSTRUCTION_TYPE.MatrixIndexing);
		String2SPInstructionType.put( "leftIndex"   	, SPINSTRUCTION_TYPE.MatrixIndexing);
		String2SPInstructionType.put( "mapLeftIndex"   	, SPINSTRUCTION_TYPE.MatrixIndexing);
		
		// Reorg Instruction Opcodes (repositioning of existing values)
		String2SPInstructionType.put( "r'"   	   , SPINSTRUCTION_TYPE.Reorg);
		String2SPInstructionType.put( "rdiag"      , SPINSTRUCTION_TYPE.Reorg);
		String2SPInstructionType.put( "rshape"     , SPINSTRUCTION_TYPE.MatrixReshape);
		String2SPInstructionType.put( "rsort"      , SPINSTRUCTION_TYPE.Reorg);
		
		String2SPInstructionType.put( "+"    , SPINSTRUCTION_TYPE.ArithmeticBinary);
		String2SPInstructionType.put( "-"    , SPINSTRUCTION_TYPE.ArithmeticBinary);
		String2SPInstructionType.put( "*"    , SPINSTRUCTION_TYPE.ArithmeticBinary);
		String2SPInstructionType.put( "/"    , SPINSTRUCTION_TYPE.ArithmeticBinary);
		String2SPInstructionType.put( "%%"   , SPINSTRUCTION_TYPE.ArithmeticBinary);
		String2SPInstructionType.put( "%/%"  , SPINSTRUCTION_TYPE.ArithmeticBinary);
		String2SPInstructionType.put( "^"    , SPINSTRUCTION_TYPE.ArithmeticBinary);
		String2SPInstructionType.put( "^2"   , SPINSTRUCTION_TYPE.ArithmeticBinary); 
		String2SPInstructionType.put( "*2"   , SPINSTRUCTION_TYPE.ArithmeticBinary); 
		String2SPInstructionType.put( "map+"    , SPINSTRUCTION_TYPE.ArithmeticBinary);
		String2SPInstructionType.put( "map-"    , SPINSTRUCTION_TYPE.ArithmeticBinary);
		String2SPInstructionType.put( "map*"    , SPINSTRUCTION_TYPE.ArithmeticBinary);
		String2SPInstructionType.put( "map/"    , SPINSTRUCTION_TYPE.ArithmeticBinary);
		String2SPInstructionType.put( "map%%"   , SPINSTRUCTION_TYPE.ArithmeticBinary);
		String2SPInstructionType.put( "map%/%"  , SPINSTRUCTION_TYPE.ArithmeticBinary);
		String2SPInstructionType.put( "map^"    , SPINSTRUCTION_TYPE.ArithmeticBinary);
		String2SPInstructionType.put( "map>"    , SPINSTRUCTION_TYPE.RelationalBinary);
		String2SPInstructionType.put( "map>="   , SPINSTRUCTION_TYPE.RelationalBinary);
		String2SPInstructionType.put( "map<"    , SPINSTRUCTION_TYPE.RelationalBinary);
		String2SPInstructionType.put( "map<="   , SPINSTRUCTION_TYPE.RelationalBinary);
		String2SPInstructionType.put( "map=="   , SPINSTRUCTION_TYPE.RelationalBinary);
		String2SPInstructionType.put( "map!="   , SPINSTRUCTION_TYPE.RelationalBinary);
		
		// Relational Instruction Opcodes 
		String2SPInstructionType.put( "=="   , SPINSTRUCTION_TYPE.RelationalBinary);
		String2SPInstructionType.put( "!="   , SPINSTRUCTION_TYPE.RelationalBinary);
		String2SPInstructionType.put( "<"    , SPINSTRUCTION_TYPE.RelationalBinary);
		String2SPInstructionType.put( ">"    , SPINSTRUCTION_TYPE.RelationalBinary);
		String2SPInstructionType.put( "<="   , SPINSTRUCTION_TYPE.RelationalBinary);
		String2SPInstructionType.put( ">="   , SPINSTRUCTION_TYPE.RelationalBinary);
		
		// REBLOCK Instruction Opcodes 
		String2SPInstructionType.put( "rblk"   , SPINSTRUCTION_TYPE.Reblock);
		String2SPInstructionType.put( "csvrblk", SPINSTRUCTION_TYPE.CSVReblock);
	
		// Spark-specific instructions
		String2SPInstructionType.put( Checkpoint.OPCODE, SPINSTRUCTION_TYPE.Checkpoint);
		
		// Builtin Instruction Opcodes 
		String2SPInstructionType.put( "log"  , SPINSTRUCTION_TYPE.Builtin);
		
		String2SPInstructionType.put( "max"  , SPINSTRUCTION_TYPE.BuiltinBinary);
		String2SPInstructionType.put( "min"  , SPINSTRUCTION_TYPE.BuiltinBinary);
		String2SPInstructionType.put( "mapmax"  , SPINSTRUCTION_TYPE.BuiltinBinary);
		String2SPInstructionType.put( "mapmin"  , SPINSTRUCTION_TYPE.BuiltinBinary);
		
		String2SPInstructionType.put( "exp"   , SPINSTRUCTION_TYPE.BuiltinUnary);
		String2SPInstructionType.put( "abs"   , SPINSTRUCTION_TYPE.BuiltinUnary);
		String2SPInstructionType.put( "sin"   , SPINSTRUCTION_TYPE.BuiltinUnary);
		String2SPInstructionType.put( "cos"   , SPINSTRUCTION_TYPE.BuiltinUnary);
		String2SPInstructionType.put( "tan"   , SPINSTRUCTION_TYPE.BuiltinUnary);
		String2SPInstructionType.put( "asin"  , SPINSTRUCTION_TYPE.BuiltinUnary);
		String2SPInstructionType.put( "acos"  , SPINSTRUCTION_TYPE.BuiltinUnary);
		String2SPInstructionType.put( "atan"  , SPINSTRUCTION_TYPE.BuiltinUnary);
		String2SPInstructionType.put( "sqrt"  , SPINSTRUCTION_TYPE.BuiltinUnary);
		String2SPInstructionType.put( "plogp" , SPINSTRUCTION_TYPE.BuiltinUnary);
		String2SPInstructionType.put( "round" , SPINSTRUCTION_TYPE.BuiltinUnary);
		String2SPInstructionType.put( "ceil"  , SPINSTRUCTION_TYPE.BuiltinUnary);
		String2SPInstructionType.put( "floor" , SPINSTRUCTION_TYPE.BuiltinUnary);
		String2SPInstructionType.put( "sprop", SPINSTRUCTION_TYPE.BuiltinUnary);
		String2SPInstructionType.put( "sigmoid", SPINSTRUCTION_TYPE.BuiltinUnary);
		
		// Parameterized Builtin Functions
		String2SPInstructionType.put( "groupedagg"	, SPINSTRUCTION_TYPE.ParameterizedBuiltin);
		String2SPInstructionType.put( "rmempty"	    , SPINSTRUCTION_TYPE.ParameterizedBuiltin);
		String2SPInstructionType.put( "replace"	    , SPINSTRUCTION_TYPE.ParameterizedBuiltin);
		String2SPInstructionType.put( "rexpand"	    , SPINSTRUCTION_TYPE.ParameterizedBuiltin);
		
		String2SPInstructionType.put( "mappend", SPINSTRUCTION_TYPE.MAppend);
		String2SPInstructionType.put( "rappend", SPINSTRUCTION_TYPE.RAppend);
		String2SPInstructionType.put( "gappend", SPINSTRUCTION_TYPE.GAppend);
		String2SPInstructionType.put( "galignedappend", SPINSTRUCTION_TYPE.GAlignedAppend);
		
		String2SPInstructionType.put( DataGen.RAND_OPCODE  , SPINSTRUCTION_TYPE.Rand);
		String2SPInstructionType.put( DataGen.SEQ_OPCODE   , SPINSTRUCTION_TYPE.Rand);
		String2SPInstructionType.put( DataGen.SAMPLE_OPCODE, SPINSTRUCTION_TYPE.Rand);
		
		//ternary instruction opcodes
		String2SPInstructionType.put( "ctable", SPINSTRUCTION_TYPE.Ternary);
		String2SPInstructionType.put( "ctableexpand", SPINSTRUCTION_TYPE.Ternary);
		
		//quaternary instruction opcodes
		String2SPInstructionType.put( WeightedSquaredLoss.OPCODE,  SPINSTRUCTION_TYPE.Quaternary);
		String2SPInstructionType.put( WeightedSquaredLossR.OPCODE, SPINSTRUCTION_TYPE.Quaternary);
		String2SPInstructionType.put( WeightedSigmoid.OPCODE,      SPINSTRUCTION_TYPE.Quaternary);
		String2SPInstructionType.put( WeightedSigmoidR.OPCODE,     SPINSTRUCTION_TYPE.Quaternary);

		//cumsum/cumprod/cummin/cummax
		String2SPInstructionType.put( "ucumack+"  , SPINSTRUCTION_TYPE.CumsumAggregate);
		String2SPInstructionType.put( "ucumac*"   , SPINSTRUCTION_TYPE.CumsumAggregate);
		String2SPInstructionType.put( "ucumacmin" , SPINSTRUCTION_TYPE.CumsumAggregate);
		String2SPInstructionType.put( "ucumacmax" , SPINSTRUCTION_TYPE.CumsumAggregate);
		String2SPInstructionType.put( "bcumoffk+" , SPINSTRUCTION_TYPE.CumsumOffset);
		String2SPInstructionType.put( "bcumoff*"  , SPINSTRUCTION_TYPE.CumsumOffset);
		String2SPInstructionType.put( "bcumoffmin", SPINSTRUCTION_TYPE.CumsumOffset);
		String2SPInstructionType.put( "bcumoffmax", SPINSTRUCTION_TYPE.CumsumOffset);

		//central moment, covariance, quantiles (sort/pick)
		String2SPInstructionType.put( "cm"     , SPINSTRUCTION_TYPE.CentralMoment);
		String2SPInstructionType.put( "cov"    , SPINSTRUCTION_TYPE.Covariance);
		String2SPInstructionType.put( "qsort"  , SPINSTRUCTION_TYPE.QSort);
		String2SPInstructionType.put( "qpick"  , SPINSTRUCTION_TYPE.QPick);
		
		String2SPInstructionType.put( "binuaggchain", SPINSTRUCTION_TYPE.BinUaggChain);
		
		String2SPInstructionType.put( "write"   , SPINSTRUCTION_TYPE.Write);
	}

	public static Instruction parseSingleInstruction (String str ) throws DMLUnsupportedOperationException, DMLRuntimeException {
		if ( str == null || str.isEmpty() )
			return null;

		SPINSTRUCTION_TYPE cptype = InstructionUtils.getSPType(str); 
		if ( cptype == null )
			// return null;
			throw new DMLUnsupportedOperationException("Invalid SP Instruction Type: " + str);
		Instruction cpinst = SPInstructionParser.parseSingleInstruction(cptype, str);
		if ( cpinst == null )
			throw new DMLRuntimeException("Unable to parse instruction: " + str);
		return cpinst;
	}
	
	public static Instruction parseSingleInstruction ( SPINSTRUCTION_TYPE sptype, String str ) throws DMLUnsupportedOperationException, DMLRuntimeException {
		
		if ( str == null || str.isEmpty() ) 
			return null;
		
		String [] parts = null;
		switch(sptype) 
		{
			// matrix multiplication instructions
			case CPMM:
				return CpmmSPInstruction.parseInstruction(str);
			case RMM:
				return RmmSPInstruction.parseInstruction(str);
			case MAPMM:
				return MapmmSPInstruction.parseInstruction(str);
			case MAPMMCHAIN:
				return MapmmChainSPInstruction.parseInstruction(str);
			case TSMM:
				return TsmmSPInstruction.parseInstruction(str);
			case PMM:
				return PmmSPInstruction.parseInstruction(str);
			case ZIPMM:
				return ZipmmSPInstruction.parseInstruction(str);
				
				
			case UaggOuterChain:
				return UaggOuterChainSPInstruction.parseInstruction(str);
				
			case AggregateUnary:
				return AggregateUnarySPInstruction.parseInstruction(str);
				
			case AggregateTernary:
				return AggregateTernarySPInstruction.parseInstruction(str);
				
			case MatrixIndexing:
				return MatrixIndexingSPInstruction.parseInstruction(str);
			case Reorg:
				return ReorgSPInstruction.parseInstruction(str);
			case ArithmeticBinary:
				return ArithmeticBinarySPInstruction.parseInstruction(str);
			case RelationalBinary:
				return RelationalBinarySPInstruction.parseInstruction(str);
			
			//ternary instructions
			case Ternary:
				return TernarySPInstruction.parseInstruction(str);
				
			//quaternary instructions
			case Quaternary:
				return QuaternarySPInstruction.parseInstruction(str);
				
			// Reblock instructions	
			case Reblock:
				return ReblockSPInstruction.parseInstruction(str);
			case CSVReblock:
				return CSVReblockSPInstruction.parseInstruction(str);
			
			case Builtin: 
				parts = InstructionUtils.getInstructionPartsWithValueType(str);
				if ( parts[0].equals("log") ) {
					if ( parts.length == 3 ) {
						// B=log(A), y=log(x)
						return (SPInstruction) BuiltinUnarySPInstruction.parseInstruction(str);
					} else if ( parts.length == 4 ) {
						// B=log(A,10), y=log(x,10)
						return (SPInstruction) BuiltinBinarySPInstruction.parseInstruction(str);
					}
				}
				else {
					throw new DMLRuntimeException("Invalid Builtin Instruction: " + str );
				}
				
			case BuiltinBinary:
				return (SPInstruction) BuiltinBinarySPInstruction.parseInstruction(str);
				
			case BuiltinUnary:
				return (SPInstruction) BuiltinUnarySPInstruction.parseInstruction(str);
				
			case ParameterizedBuiltin:
				return (SPInstruction) ParameterizedBuiltinSPInstruction.parseInstruction(str);
				
			case MatrixReshape:
				return (SPInstruction) MatrixReshapeSPInstruction.parseInstruction(str);
				
			case MAppend:
				return (SPInstruction) AppendMSPInstruction.parseInstruction(str);
			
			case GAppend:
				return (SPInstruction) AppendGSPInstruction.parseInstruction(str);
			
			case GAlignedAppend:
				return (SPInstruction) AppendGAlignedSPInstruction.parseInstruction(str);
				
			case RAppend:
				return (SPInstruction) AppendRSPInstruction.parseInstruction(str);
				
			case Rand:
				return (SPInstruction) RandSPInstruction.parseInstruction(str);
				
			case QSort: 
				return (SPInstruction) QuantileSortSPInstruction.parseInstruction(str);
			
			case QPick: 
				return (SPInstruction) QuantilePickSPInstruction.parseInstruction(str);
			
			case Write:
				return (SPInstruction) WriteSPInstruction.parseInstruction(str);
				
			case CumsumAggregate:
				return CumulativeAggregateSPInstruction.parseInstruction(str);
				
			case CumsumOffset:
				return CumulativeOffsetSPInstruction.parseInstruction(str); 
		
			case CentralMoment:
				return CentralMomentSPInstruction.parseInstruction(str);
			
			case Covariance:
				return CovarianceSPInstruction.parseInstruction(str);
			
			case BinUaggChain:
				return BinUaggChainSPInstruction.parseInstruction(str);
				
			case Checkpoint:
				return CheckpointSPInstruction.parseInstruction(str);
				
			case INVALID:
			default:
				throw new DMLUnsupportedOperationException("Invalid SP Instruction Type: " + sptype );
		}
	}
	
	public static SPInstruction[] parseMixedInstructions ( String str ) throws DMLUnsupportedOperationException, DMLRuntimeException {
		if ( str == null || str.isEmpty() )
			return null;
		
		Instruction[] inst = InstructionParser.parseMixedInstructions(str);
		SPInstruction[] cpinst = new SPInstruction[inst.length];
		for ( int i=0; i < inst.length; i++ ) {
			cpinst[i] = (SPInstruction) inst[i];
		}
		
		return cpinst;
	}
	
	
}
