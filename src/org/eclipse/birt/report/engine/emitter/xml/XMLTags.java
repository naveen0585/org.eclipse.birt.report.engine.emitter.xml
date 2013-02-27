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

public class XMLTags {
	public static final String TAG_CR = "\n" ;

	static String valueTag = "??value";
	
	//static String label = "??label";
	//static String text = "??text";
	//static String image = "??image";
	//static String data = "??data";
	
	static String labelControl = "label";
	static String textControl = "text";
	static String imageControl = "image";
	static String dataControl = "data";
	static String reportControl = "report";
	static String startControl = "start";
	static String endControl = "end";

	static String[] iPropList = {"Bookmark","Height","Hyperlink","ImageMap","InlineStyle","MIMEType","Name","Style","TOC","URI","Width","X","Y"};
	static String[] dPropList = {"Bookmark","Height","Hyperlink","InlineStyle","Name","Style","TOC","Width","X","Y","LabelText","LabelKey"};
	static String[] lPropList = {"Bookmark","Height","Hyperlink","InlineStyle","Name","TOC","Width","X","Y","LabelText","LabelKey"};

	static String[] tPropList = {"Bookmark","Height","Hyperlink","InlineStyle","Name","Style","TOC","Width","X","Y"};
	static String[] rPropList = {"TotalPages", "TOCTree", "Name"};
	static String[] rowPropList = {"rowID"};
}
