package operator.useq

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

import operator.IOOperator;
import operator.OperationFailedException;

import org.w3c.dom.NodeList;

import pipeline.Pipeline;
import buffer.BAMFile;
import buffer.FastQFile;
import buffer.FileBuffer;
import buffer.ReferenceFile;

/**
 * Implements USeq's SamFilter program.
 * Parses a SAM file into spanning, single, and soft-masked alignment groups.
 * Also generates counts for each possible translocation
 *
 *   
 * @author daniel
 * 
 */

