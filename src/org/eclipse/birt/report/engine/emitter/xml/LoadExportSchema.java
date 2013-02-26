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
import java.io.BufferedReader;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class LoadExportSchema{
	
	protected String fileName = "";

	protected String startTag = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
	protected String textTag = "<text>??value</text>";
	protected String imageTag = "<image>??value</image>";
	protected String dataTag = "<data name=\"??Name\">??value</data>";
	protected String labelTag = "<label>??value</label>";
	protected String endTag = "</report>";
	protected String reportTag = "<report>";
		
/**
 * start=<?xml version="1.0" encoding="UTF-8"?>
 * report=<report:??name>
 * label=<label:??name>??value</label>
* text=<text:??name>??value</text>
* image=<image:??name>??value</image>
* data=<data:??name>??value</data>
* end=</report>
 */
	
	public LoadExportSchema(String fileName)
	{
		if ( fileName.length() > 0 )
		{
			this.fileName = fileName;
			readSchemaFile();
		}
	}

	public String getExportLabelTag()
	{
		return labelTag;
	}
	
	public String getExportImageTag()
	{
		return imageTag;
	}

	public String getExportDataTag()
	{
		return dataTag;
	}
	public String getExportReportTag()
	{
		return reportTag;
	}
	public String getExportStartTag()
	{
		return startTag;
	}
	public String getExportTextTag()
	{
		return textTag;
	}
	
	public String getExportEndTag()
	{
		return endTag;
	}

	public boolean isPropertyRequired( String propValue,  String controlTag )
	{
		if  ( (controlTag.toLowerCase()).indexOf( propValue.toLowerCase() ) > 0 )
		{
				return true;		
		}
		return false;
	}

	private void readSchemaFile()
	{
    //declared here only to make visible to finally clause
	    BufferedReader input = null;
	    try {
			 //use buffering, reading one line at a time
			 //FileReader always assumes default encoding is OK!
			 input = new BufferedReader( new FileReader(fileName) );
			      String line = null; //not declared within while loop
			      /*
			      * it returns the content of a line MINUS the newline.
			      * it returns null only for the END of the stream.
			      * it returns an empty String if two newlines appear in a row.
			      */
			      while (( line = input.readLine()) != null){
			    	int pos = line.indexOf("=");
			    	if ( pos > 0 )
			    	{
			    		String index = line.substring(0, pos );
			    		String indexTag = line.substring(pos + 1, line.length());
			    		
			    		if ( index.equalsIgnoreCase( XMLTags.labelControl ) )
			    		{
			    			labelTag = indexTag;
			    		}
			    		if ( index.equalsIgnoreCase( XMLTags.textControl ) )
			    		{
			    			textTag = indexTag;
			    		}			    					    		
			    		if ( index.equalsIgnoreCase( XMLTags.imageControl ) )
			    		{
			    			imageTag = indexTag;
			    		}
			    		if ( index.equalsIgnoreCase( XMLTags.dataControl ) )
			    		{
			    			dataTag = indexTag;
			    		}
			    		if ( index.equalsIgnoreCase( XMLTags.startControl ) )
			    		{
			    			startTag = indexTag;
			    		}
			    		if ( index.equalsIgnoreCase( XMLTags.endControl ) )
			    		{
			    			endTag = indexTag;
			    		}
			    		if ( index.equalsIgnoreCase( XMLTags.reportControl ) )
			    		{
			    			reportTag = indexTag;
			    		}    		
			    	}//end of if (pos)
  			      }//end of while
			    }
			    catch (FileNotFoundException ex) {
			      ex.printStackTrace();
			    }
			    catch (IOException ex){
			      ex.printStackTrace();
			    }
			    finally {
			      try {
			        if (input!= null) {
			          //flush and close both "input" and its underlying FileReader
			          input.close();
			        }
			      }
			      catch (IOException ex) {
			        ex.printStackTrace();
			      }
			    }
	}

	/**
	 * Replace the escape characters.
	 * 
	 * @param s
	 *            The string needs to be replaced.
	 * @param whiteespace
	 *            A
	 *            <code>boolean<code> value indicating if the white space character should be converted or not. 
	 * @return The replaced string.
	 */
	protected String getEscapedStr( String s, boolean whitespace )
	{
		StringBuffer result = null;
		int spacePos = 1;
		char[] s2char = s.toCharArray( );

		for ( int i = 0, max = s2char.length, delta = 0; i < max; i++ )
		{
			char c = s2char[i];
			String replacement = null;			
			//The first and the last characters are converted to Entity.
			if ( whitespace && c == ' ' )
			{
				boolean replace = false;
				if(spacePos % 2 == 1)
				{
					replace = true;
				}
				else
				{
					char last = ( i - 1 >= 0 ? s2char[i - 1] : '\n' );
					char next = ( i + 1 < max ? s2char[i + 1] : '\n' );
					char nextNext = ( i + 2 < max ? s2char[i + 2] : '\n' );
					if(last=='\n' || next=='\n' ||(next=='\r' && nextNext=='\n')  )
					{
						replace = true;
					}
				}
				if(replace)
				{
					replacement = "&nbsp;"; //$NON-NLS-1$
				}
				spacePos++;				
			}
			else
			{
				spacePos = 0;
			}
			
			// Filters the char not defined.
			if ( !( c == 0x9 || c == 0xA || c == 0xD
					|| ( c >= 0x20 && c <= 0xD7FF ) || ( c >= 0xE000 && c <= 0xFFFD ) ) )
			{
				// Ignores the illegal character.
				replacement = ""; //$NON-NLS-1$
/*				log.log( Level.WARNING,
						"Ignore the illegal XML character: 0x{0};", Integer //$NON-NLS-1$
								.toHexString( c ) );*/
			}
			else if ( c == '&' )
			{
				replacement = "&amp;"; //$NON-NLS-1$
			}
			else if ( c == '<' )
			{
				replacement = "&lt;"; //$NON-NLS-1$
			}
			else if ( c == '>' )
			{
				replacement = "&gt;"; //$NON-NLS-1$
			}
			else if ( c == '\t' )
			{
				replacement = "&nbsp;"; //$NON-NLS-1$
			}
			else if ( c == '\r' )
			{
				int n = i + 1;
				if ( n < max && s2char[n] == '\n' )
				{
					replacement = ""; //$NON-NLS-1$
				}
			}
			else if ( c == '\n' )
			{
				replacement = "<br>"; //$NON-NLS-1$
			}
			else if ( c >= 0x80 )
			{
				replacement = "&#x" + Integer.toHexString( c ) + ';'; //$NON-NLS-1$ 
			}

			if ( replacement != null )
			{
				if ( result == null )
				{
					result = new StringBuffer( s );
				}
				result.replace( i + delta, i + delta + 1, replacement );
				delta += ( replacement.length( ) - 1 );
			}
		}
		if ( result == null )
		{
			return s;
		}
		return result.toString( );
	}
		
	
    public static void main (String argv []) {
    	
    	LoadExportSchema ls = new LoadExportSchema("");
    	ls.readSchemaFile();
    	// String dt = ls.getExportDataTag();
    	// boolean prop = ls.isPropertyRequired("name", dt);
    	// boolean prop = ls.isPropertyRequired("dsds", dt);
   	
      } //end of main
}
