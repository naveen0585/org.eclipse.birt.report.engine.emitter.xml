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

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
// import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.xerces.impl.dv.util.Base64;
import org.eclipse.birt.report.engine.api.IRenderOption;
import org.eclipse.birt.report.engine.api.RenderOption;
import org.eclipse.birt.report.engine.content.ICellContent;
import org.eclipse.birt.report.engine.content.IContainerContent;
import org.eclipse.birt.report.engine.content.IDataContent;

import org.eclipse.birt.report.engine.content.IForeignContent;
import org.eclipse.birt.report.engine.content.IImageContent;
import org.eclipse.birt.report.engine.content.ILabelContent;
import org.eclipse.birt.report.engine.content.IPageContent;
import org.eclipse.birt.report.engine.content.IReportContent;
import org.eclipse.birt.report.engine.content.IRowContent;

import org.eclipse.birt.report.engine.content.ITableContent;
import org.eclipse.birt.report.engine.content.ITextContent;

import org.eclipse.birt.report.engine.emitter.ContentEmitterAdapter;
import org.eclipse.birt.report.engine.emitter.IEmitterServices;

import org.eclipse.birt.report.engine.presentation.ContentEmitterVisitor;

public class XMLReportEmitter extends ContentEmitterAdapter {
	
	public final static String APPCONTEXT_XML_RENDER_CONTEXT = "XML_RENDER_CONTEXT"; //$NON-NLS-1$

	/**
	 * the output format
	 */
	public static final String OUTPUT_FORMAT_XML = "XML"; //$NON-NLS-1$
	/**
	 * the default target report file name
	 */
	public static final String REPORT_FILE = "report.xml"; //$NON-NLS-1$

	/**
	 * output stream
	 */
	protected OutputStream out = null;

	/**
	 * the report content
	 */
	protected IReportContent report;


	/**
	 * the render options
	 */
	protected IRenderOption renderOption;

	/**
	 * should output the page header & footer
	 */
	protected boolean outputMasterPageContent = true;


	/**
	 * the <code>CSVWriter<code> object that is used to output CSV content
	 */
	protected XMLFileWriter writer;

	/**
	 * An Log object that <code>XMLReportEmitter</code> use to log the error,
	 * debug, information messages.
	 */
	protected static Logger logger = Logger.getLogger( XMLReportEmitter.class
			.getName( ) );

	/**
	 * emitter services
	 */
	protected IEmitterServices services;

	/**
	 * content visitor that is used to handle page header/footer
	 */
	protected ContentEmitterVisitor contentVisitor;
	
	/**
	 * loads the properties from the xml schema file
	 */	
	protected LoadExportSchema exportSchema = null;

	/**
	 * the constructor
	 */
	public XMLReportEmitter( )
	{
		contentVisitor = new ContentEmitterVisitor( this );
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.birt.report.engine.emitter.IContentEmitter#initialize(org.eclipse.birt.report.engine.emitter.IEmitterServices)
	 */
	public void initialize( IEmitterServices services )
	{
		this.services = services;
		
		Object fd = services.getOption( RenderOption.OUTPUT_FILE_NAME );
		File file = null;
		try
		{
			if ( fd != null )
			{
				file = new File( fd.toString( ) );
				File parent = file.getParentFile( );
				if ( parent != null && !parent.exists( ) )
				{
					parent.mkdirs( );
				}
				out = new BufferedOutputStream( new FileOutputStream( file ) );
			}
		}
		catch ( FileNotFoundException e )
		{
			logger.log( Level.WARNING, e.getMessage( ), e );
		}

		if ( out == null )
		{
			Object value = services.getOption( RenderOption.OUTPUT_STREAM );
			if ( value != null && value instanceof OutputStream )
			{
				out = (OutputStream) value;
			}
			else
			{
				try
				{
					file = new File( REPORT_FILE );
					out = new BufferedOutputStream( new FileOutputStream( file ) );
				}
				catch ( FileNotFoundException e )
				{
					
					logger.log( Level.SEVERE, e.getMessage( ), e );
				}
			}
		}

		writer = new XMLFileWriter( );
	}

	/**
	 * @return the <code>Report</code> object.
	 */
	public IReportContent getReport( )
	{
		return report;
	}

	public String getOutputFormat( )
	{
		return OUTPUT_FORMAT_XML;
	}

	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.birt.report.engine.emitter.IContentEmitter#start(org.eclipse.birt.report.engine.content.IReportContent)
	 */
	public void start( IReportContent report )
	{
		logger.log( Level.FINE, "[XMLReportEmitter] Start emitter." ); //$NON-NLS-1$
		
		String fileName = report.getDesign().getReportDesign().getFileName();

		int pos = fileName.indexOf("/"); 
		String fn = fileName.substring(pos+1,fileName.length());
		fileName = fn;
		if (fileName.length() > 0) {
			pos =  fileName.lastIndexOf(".");
			if ( pos > 0 )
				fileName = fileName.substring(0, pos);
			
				fileName = fileName + ".xmlemitter";
				pos = fileName.lastIndexOf("/");
				String propFileName = fileName.substring( pos+1 , fileName.length() );
				String resourceFolder = report.getDesign().getReportDesign().getResourceFolder();
				if ( fileExists(resourceFolder + "/" + propFileName))
					exportSchema = new LoadExportSchema( resourceFolder + "/" + propFileName );
				else if ( fileExists(fileName))
						exportSchema = new LoadExportSchema( fileName );
					else exportSchema = new LoadExportSchema( "" ); // no schema file, load the defaults
		}
		this.report = report;
		writer.open( out, "UTF-8" ); //$NON-NLS-1$

		writer.startWriter( );
		
		writer.closeTag( exportSchema.getExportStartTag());
		writer.closeTag( XMLTags.TAG_CR );
		
		String rp = exportSchema.getExportReportTag();
		for (int i = 0;i < XMLTags.rPropList.length;i++)
		{
			if (exportSchema.isPropertyRequired(XMLTags.rPropList[i], rp))
			{
				String propValue = getReportPropValue(i,report);
				rp = replaceTag( rp, "??"+XMLTags.rPropList[i], propValue );
			}
		}
		writer.writeCode( rp );
		writer.closeTag( XMLTags.TAG_CR );
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.birt.report.engine.emitter.IContentEmitter#end(org.eclipse.birt.report.engine.content.IReportContent)
	 */
	public void end( IReportContent report )
	{
		logger.log( Level.FINE, "[XMLReportEmitter] End report." ); //$NON-NLS-1$
		writer.closeTag( exportSchema.getExportEndTag());

		writer.endWriter( );
		writer.close( );
		if( out != null )
		{
			try
			{
				out.close( );
			}
			catch ( IOException e )
			{
				logger.log( Level.WARNING, e.getMessage( ), e );
			}
		}	
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.birt.report.engine.emitter.IContentEmitter#startPage(org.eclipse.birt.report.engine.content.IPageContent)
	 */
	public void startPage( IPageContent page )
	{

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.birt.report.engine.emitter.IContentEmitter#endPage(org.eclipse.birt.report.engine.content.IPageContent)
	 */
	public void endPage( IPageContent page )
	{

	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.birt.report.engine.emitter.IContentEmitter#startTable(org.eclipse.birt.report.engine.content.ITableContent)
	 */
	public void startTable( ITableContent table )
	{
		assert table != null;
		logger.log( Level.FINE, "[XMLTableEmitter] Start table" ); 		
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.birt.report.engine.emitter.IContentEmitter#endTable(org.eclipse.birt.report.engine.content.ITableContent)
	 */
	public void endTable( ITableContent table )
	{

		logger.log( Level.FINE, "[XMLReportEmitter] End table" ); //$NON-NLS-1$
	
	}

	public void startRow( IRowContent row )
	{
		assert row != null;
		
	}



	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.birt.report.engine.emitter.IContentEmitter#endRow(org.eclipse.birt.report.engine.content.IRowContent)
	 */
	public void endRow( IRowContent row )
	{
		
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.birt.report.engine.emitter.IContentEmitter#startCell(org.eclipse.birt.report.engine.content.ICellContent)
	 */
	public void startCell( ICellContent cell )
	{
		
		logger.log( Level.FINE, "[XMLReportEmitter] Start cell." ); //$NON-NLS-1$

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.birt.report.engine.emitter.IContentEmitter#endCell(org.eclipse.birt.report.engine.content.ICellContent)
	 */
	public void endCell( ICellContent cell )
	{
		logger.log( Level.FINE, "[XMLReportEmitter] End cell." ); //$NON-NLS-1$
	

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.birt.report.engine.emitter.IContentEmitter#startContainer(org.eclipse.birt.report.engine.content.IContainerContent)
	 */
	public void startContainer( IContainerContent container )
	{
		
		logger.log( Level.FINE, "[XMLReportEmitter] Start container" ); //$NON-NLS-1$

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.birt.report.engine.emitter.IContentEmitter#endContainer(org.eclipse.birt.report.engine.content.IContainerContent)
	 */
	public void endContainer( IContainerContent container )
	{
		logger.log( Level.FINE, "[CSVContainerEmitter] End container" ); //$NON-NLS-1$
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.birt.report.engine.emitter.IContentEmitter#startText(org.eclipse.birt.report.engine.content.ITextContent)
	 */
	public void startText( ITextContent text )
	{

		logger.log( Level.FINE, "[XMLReportEmitter] Start text" ); //$NON-NLS-1$
		String txt = exportSchema.getExportLabelTag();
		// label=<label:??name>??value</label>
		// int len =  XMLTags.lPropList.length;
		for (int i = 0;i < XMLTags.lPropList.length;i++)
		{
			if (exportSchema.isPropertyRequired(XMLTags.lPropList[i], txt))
			{
				String propValue = getTextPropValue(i,text);
				txt = replaceTag( txt, "??"+XMLTags.tPropList[i], propValue );
			}
		}		
		
		String textValue = text.getText( );
		writer.writeCode( replaceTag( txt, XMLTags.valueTag, textValue ) );

		writer.closeTag( XMLTags.TAG_CR );

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.birt.report.engine.emitter.IContentEmitter#startForeign(org.eclipse.birt.report.engine.content.IForeignContent)
	 */
	public void startForeign( IForeignContent foreign )
	{

	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.birt.report.engine.emitter.IContentEmitter#startLabel(org.eclipse.birt.report.engine.content.ILabelContent)
	 */
	public void startLabel( ILabelContent label )
	{   
//		static String[] lPropList = {"Bookmark","Height","Hyperlink","InlineStyle","Name","TOC","Width","X","Y"};
		
		String lbl = exportSchema.getExportLabelTag();
		// label=<label:??name>??value</label>
		// int len =  XMLTags.lPropList.length;
		for (int i = 0;i < XMLTags.lPropList.length;i++)
		{
			if (exportSchema.isPropertyRequired(XMLTags.lPropList[i], lbl))
			{
				String propValue = getLabelPropValue(i,label);
				lbl = replaceTag( lbl, "??"+XMLTags.lPropList[i], propValue );
			}
		}		
		
		//startText( label, lbl );
		String textValue = label.getText( );
		writer.writeCode( replaceTag( lbl, XMLTags.valueTag, textValue ) );

		writer.closeTag( XMLTags.TAG_CR );
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.birt.report.engine.emitter.IContentEmitter#startData(org.eclipse.birt.report.engine.content.IDataContent)
	 */
	public void startData( IDataContent data )
	{
		//static String[] dPropList = {"Bookmark","Height","Hyperlink","InlineStyle","Name","Style","TOC","Width","X","Y"};
		
		String dt = exportSchema.getExportDataTag();
		//label=<label:??name>??value</label>
		
		for (int i = 0;i < XMLTags.dPropList.length;i++)
		{
			if (exportSchema.isPropertyRequired(XMLTags.dPropList[i], dt))
			{
				String propValue = getDataPropValue(i,data);
				dt = replaceTag( dt, "??"+XMLTags.dPropList[i], propValue );
			}
		}		
//		startText( data, dt );
		String textValue = data.getText( );
		writer.writeCode( replaceTag( dt, XMLTags.valueTag, textValue ) );

		writer.closeTag( XMLTags.TAG_CR );
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.birt.report.engine.emitter.IContentEmitter#startImage(org.eclipse.birt.report.engine.content.IImageContent)
	 */
	
	public void startImage( IImageContent image )
	{
		byte[] data = parseImage(image);
		
		String pic2Text = Base64.encode(data);
			//new sun.misc.BASE64Encoder().encode(data);
		String im = exportSchema.getExportImageTag();
		
		for (int i = 0;i < XMLTags.iPropList.length;i++)
		{
			if (exportSchema.isPropertyRequired(XMLTags.iPropList[i], im))
			{
				String propValue = getImagePropValue(i,image);
				im = replaceTag( im, "??"+XMLTags.iPropList[i], propValue );
			}
		}
		writer.writeCode( replaceTag( im, XMLTags.valueTag, pic2Text ) );
		writer.closeTag( XMLTags.TAG_CR );
	}

	/**
	 * handle style image
	 * 
	 * @param uri
	 *            uri in style image
	 * @return
	 */
	public String handleStyleImage( String uri )
	{
		String id = null;
		return id;
	}
	
	
	/**
	 * 
	 * @param fileName
	 * @return
	 */
	private boolean fileExists( String fileName )
	{
		File file = new File( fileName );
		return file.exists();
	}
	
	/**
	 * 
	 * @param is
	 * @return
	 * @throws Exception
	 */
	private byte[] getImage(InputStream is) throws Exception {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		int cnt;
		byte[] buf = new byte[4096];

		while ((cnt = is.read(buf)) >= 0) {
			baos.write(buf, 0, cnt);
		}
        is.close();
        baos.close();
		return baos.toByteArray();
	}
/**
 * 
 * @param image
 * @return image data
 */
	private byte[] parseImage(IImageContent image) {
		byte[] data = null;

		if (image.getData() != null) {
			data = image.getData();
		} else {
			String url = image.getURI();

			if (url == null) {
				return null;
			}
			try {
				URL ourl = new URL(url);

				return getImage(ourl.openStream());
			} catch (Exception e) {
				logger.log(Level.WARNING, e.getMessage(), e);
			}
		}
		return data;
	}
	/**
	 * 
	 * @param exportTag
	 * @param oldPattern
	 * @param newPattern
	 * @return the replaced string
	 */
	private String replaceTag( String exportTag, String oldPattern, String newPattern)
	{
		if ( oldPattern.equals("") ) {
	        throw new IllegalArgumentException("Old pattern must have content.");
	     }

	     final StringBuffer result = new StringBuffer();
	     //startIdx and idxOld delimit various chunks of exportTag; these
	     //chunks always end where oldPattern begins
	     int startIdx = 0;
	     int idxOld = 0;
	     while ((idxOld = (exportTag.toLowerCase()).indexOf(oldPattern.toLowerCase(), startIdx)) >= 0) {
	       //grab a part of exportTag which does not include oldPattern
	       result.append( exportTag.substring(startIdx, idxOld) );
	       //add newPattern to take place of oldPattern
	       result.append( newPattern );

	       //reset the startIdx to just after the current match, to see
	       //if there are any further matches
	       startIdx = idxOld + oldPattern.length();
	     }
	     //the final chunk will go to the end of exportTag
	     result.append( exportTag.substring(startIdx) );
	     return result.toString();
	}
	/**
	 * 
	 * @param property
	 * @param data
	 * @return String property value
	 */
	private String getImagePropValue( int property, IImageContent image)
	{
		String propValue;
		//static String[] iPropList = {"Bookmark","Height","Hyperlink","ImageMap",
		//"InlineStyle","MIMEType","Name","Style","TOC","URI","Width","X","Y"};
		
		switch (property) { 
	    case 0: // "Bookmark":
	    	propValue = image.getBookmark();
	    	break;
	    case 1: // "Height":
	    	if ( image.getHeight() != null )
	    		propValue = image.getHeight().toString();
	    	else 
	    		propValue = "";
	    	break;
	    case 2: //"Hyperlink":	    	
	    		if ( image.getHyperlinkAction() != null )
	    			propValue = image.getHyperlinkAction().getHyperlink();
	    		else propValue = "";
	    	break;
	    case 3: // "ImageMap":
	    	if ( image.getImageMap() != null )
	    		propValue = image.getImageMap().toString();
	    	else 
	    		propValue = "";
	    	break;	    	
	    case 4: //"InlineStyle":
	    	propValue = image.getInlineStyle().getCssText();
	    	break;
	    case 5: //"MIMEType":
	    	propValue = image.getMIMEType();
	    	break;
	    case 6: //"Name":
	    	propValue = image.getName();
	    	break;
	    case 7: //"Style":
	    	propValue = image.getStyle().getCssText();
	    	break;    	
	    case 8: //"TOC":
	    	if ( image.getTOC() != null )
	    		propValue = image.getTOC().toString();
	    	else propValue = "";
	    	break;
	    case 9: //"URI":
	    	if ( image.getTOC() != null )
	    		propValue = image.getTOC().toString();
	    	else propValue = "";
	    	break;
	    	
	    case 10: //"Width":
	    	if ( image.getWidth() != null )
	    		propValue = image.getWidth().toString();
	    	else propValue = "";
	    	break;
	    case 11: //"X":
	    	if ( image.getX() != null )
	    		propValue = image.getX().toString();
	    	else 
	    		propValue = "";
	    	break;
	    case 12: //"Y": 
	    	if ( image.getY() != null )
	    		propValue = image.getY().toString();
	    	else 
	    		propValue = "";	    	break;	    
	    default: propValue = ""; 
    		break;
	}
		if ( propValue == null ) 
			propValue = "";

		return propValue;
	}
/**
 * 
 * @param property
 * @param data
 * @return String - property value
 */	
	private String getDataPropValue( int property, IDataContent data)
	{
		String propValue;
		//static String[] dPropList = {"Bookmark","Height","Hyperlink","InlineStyle","Name","Style","TOC","Width","X","Y"};
		
		switch (property) { 
	    case 0: // "Bookmark":
	    	propValue = data.getBookmark();
	    	break;
	    case 1: // "Height":
	    	if ( data.getHeight() != null )
	    		propValue = data.getHeight().toString();
	    	else 
	    		propValue = "";
	    	break;
	    case 2: //"Hyperlink":	    	
	    		if ( data.getHyperlinkAction() != null )
	    			propValue = data.getHyperlinkAction().getHyperlink();
	    		else propValue = "";
	    	break;
	    case 3: //"InlineStyle":
	    	propValue = data.getInlineStyle().getCssText();
	    	break;
	    case 4: //"Name":
	    	propValue = data.getName();
	    	break;
	    case 5: //"Style":
	    	propValue = data.getStyle().getCssText();
	    	break;    	
	    case 6: //"TOC":
	    	if ( data.getTOC() != null )
	    		propValue = data.getTOC().toString();
	    	else propValue = "";
	    	break;
	    case 7: //"Width":
	    	if ( data.getWidth() != null )
	    		propValue = data.getWidth().toString();
	    	else propValue = "";
	    	break;
	    case 8: //"X":
	    	if ( data.getX() != null )
	    		propValue = data.getX().toString();
	    	else 
	    		propValue = "";
	    	break;
	    case 9: //"Y": 
	    	if ( data.getY() != null )
	    		propValue = data.getY().toString();
	    	else 
	    		propValue = "";	    	break;	    
	    default: propValue = ""; 
    		break;
	}
		if ( propValue == null ) 
			propValue = "";

		return propValue;
	}
	/**
	 * 
	 * @param property
	 * @param text
	 * @return
	 */
	private String getTextPropValue( int property, ITextContent text)
	{
		String propValue;
	//	static String[] tPropList = {"Bookmark","Height","Hyperlink","InlineStyle",
		//"Name","Style","TOC","Width","X","Y"};
	
		switch (property) { 
	    case 0: // "Bookmark":
	    	propValue = text.getBookmark();
	    	break;
	    case 1: // "Height":
	    	if ( text.getHeight() != null )
	    		propValue = text.getHeight().toString();
	    	else 
	    		propValue = "";
	    	break;
	    case 2: //"Hyperlink":	    	
	    		if ( text.getHyperlinkAction() != null )
	    			propValue = text.getHyperlinkAction().getHyperlink();
	    		else propValue = "";
	    	break;
	    case 3: //"InlineStyle":
	    	propValue = text.getInlineStyle().getCssText();
	    	break;
	    case 4: //"Name":
	    	propValue = text.getName();
	    	break;
	    case 5: //"Style":
	    	propValue = text.getStyle().getCssText();
	    	break; 
	    case 6: //"TOC":
	    	if ( text.getTOC() != null )
	    		propValue = text.getTOC().toString();
	    	else propValue = "";
	    	break;
	    case 7: //"Width":
	    	if ( text.getWidth() != null )
	    		propValue = text.getWidth().toString();
	    	else propValue = "";
	    	break;
	    case 8: //"X":
	    	if ( text.getX() != null )
	    		propValue = text.getX().toString();
	    	else 
	    		propValue = "";
	    	break;
	    case 9: //"Y": 
	    	if ( text.getY() != null )
	    		propValue = text.getY().toString();
	    	else 
	    		propValue = "";	    	break;	    
	    default: propValue = ""; 
    		break;
	}
		if ( propValue == null ) 
			propValue = "";
		return propValue;
	
	}
/**
 * 
 * @param property
 * @param label
 * @return
 */	
	private String getLabelPropValue( int property, ILabelContent label)
	{
		String propValue;
		
		switch (property) { 
	    case 0: // "Bookmark":
	    	propValue = label.getBookmark();
	    	break;
	    case 1: // "Height":
	    	if ( label.getHeight() != null )
	    		propValue = label.getHeight().toString();
	    	else 
	    		propValue = "";
	    	break;
	    case 2: //"Hyperlink":	    	
	    		if ( label.getHyperlinkAction() != null )
	    			propValue = label.getHyperlinkAction().getHyperlink();
	    		else propValue = "";
	    	break;
	    case 3: //"InlineStyle":
	    	propValue = label.getInlineStyle().getCssText();
	    	break;
	    case 4: //"Name":
	    	propValue = label.getName();
	    	break;
	    case 5: //"TOC":
	    	if ( label.getTOC() != null )
	    		propValue = label.getTOC().toString();
	    	else propValue = "";
	    	break;
	    case 6: //"Width":
	    	if ( label.getWidth() != null )
	    		propValue = label.getWidth().toString();
	    	else propValue = "";
	    	break;
	    case 7: //"X":
	    	if ( label.getX() != null )
	    		propValue = label.getX().toString();
	    	else 
	    		propValue = "";
	    	break;
	    case 8: //"Y": 
	    	if ( label.getY() != null )
	    		propValue = label.getY().toString();
	    	else 
	    		propValue = "";	    	break;	    
	    default: propValue = ""; 
    		break;
	}
		if ( propValue == null ) 
			propValue = "";
		return propValue;
	}
	/**
	 * 
	 * @param property
	 * @param report
	 * @return
	 */
	@SuppressWarnings("deprecation")
	private String getReportPropValue(int property, IReportContent report)
	{
		String propValue;
		//static String[] rPropList = {"TotalPages", "TOCTree"};

		switch (property) { 
	    case 0: // "TotalPages":
	    	Long pgCount = report.getTotalPage();
	    	propValue = pgCount.toString();
	    	break;
	    case 1: // "TOCTree":
	    	if ( report.getTOC() != null )
	    		propValue = report.getTOC().toString();
	    	else 
	    		propValue = "";
	    case 2: //"Name":
	    	String fileName = report.getDesign().getReportDesign().getFileName();
	    	int xi = fileName.lastIndexOf("C:");
	    	if (xi < 0)
	    	{
	    		propValue = "";
	    	} else
	    	{	    	
	    		propValue = fileName.substring( xi ,fileName.length());	
	    	}
	    	break;
	     
	    default: propValue = ""; 
    		break;
	}
		if ( propValue == null ) 
			propValue = "";
		return propValue;
	}

}
