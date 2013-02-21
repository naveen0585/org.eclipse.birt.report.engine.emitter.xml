/*******************************************************************************
 * Copyright (c) 2010 Actuate Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Actuate Corporation  - initial API and implementation
 *******************************************************************************/

package org.eclipse.birt.report.engine.emitter.xml;

import org.eclipse.birt.report.engine.emitter.XMLWriter;

public class XMLFileWriter extends XMLWriter {
	/**
	 * Creates a CSVWriter using this constructor.
	 */
	public XMLFileWriter( )
	{
	}

	/**
	 * Outputs java script code.
	 * 
	 * @param code
	 *            a line of code
	 */
	public void writeCode( String code )
	{
		
//		super.printWriter.print( code );
		print( code );
	}

	// Overrides
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.birt.report.engine.emitter.XMLWriter#startWriter()
	 */
	public void startWriter( )
	{

	}


	/**
	 * Close the tag
	 * 
	 * @param tagName
	 *            tag name
	 */
	public void closeTag( String tagName )
	{
		//super.printWriter.print( tagName );	
		print( tagName );	

	}

	/**
	 * Close the tag whose end tag is forbidden say, "br".
	 * 
	 * @param tagName
	 *            tag name
	 */
	public void closeNoEndTag( )
	{

	}

}
