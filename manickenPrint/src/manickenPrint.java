/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2008 Ben Fry and Casey Reas
  Copyright (c) 2020 Jannik LS Svensson (1984)- Sweden

  This program is free software; you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation; either version 2 of the License, or
  (at your option) any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the Free Software Foundation,
  Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package com.manicken;

import javax.print.PrintService;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.swing.JMenuItem;
import javax.swing.JTextPane;
import javax.swing.JFrame;
import javax.swing.JOptionPane; // used by print preview dialog
import javax.swing.JPanel;
import javax.swing.JButton;
import java.awt.print.*;
import javax.print.attribute.standard.*;
import java.text.MessageFormat;

import processing.app.Editor;
import processing.app.tools.Tool;

import processing.app.tools.MyDiscourseFormat;
import static processing.app.I18n.tr; // translate (multi language support)

import com.manicken.CustomMenu;

/**
 * 
 */
public class manickenPrint implements Tool
{
	int SpacesAfterLineNumber = 4;

	Editor editor;// for the plugin

	String thisToolMenuTitle = "Manicken Print";

	public void init(Editor editor) { // required by tool loader
		this.editor = editor;

		// workaround to make sure that init is run after the Arduino IDE gui has loaded
		// otherwise any System.out(will never be shown at the init phase) 
		editor.addWindowListener(new java.awt.event.WindowAdapter() {
			public void windowOpened(java.awt.event.WindowEvent e) {
			  init();
			}
		});
	}

	public void run() {// required by tool loader
		// this is not used when using custom menu (see down @initMenu())
	}

	public String getMenuTitle() {// required by tool loader
		return thisToolMenuTitle;
	}
	

	/**
	 * This is the code that runs after the Arduino IDE GUI has been loaded
	 */
	private void init() {
		try{
			CustomMenu cm = new CustomMenu(this.editor, thisToolMenuTitle, 
				new JMenuItem[] {
					/*
					CustomMenu.Item("Print Color", event -> Print(false, true)),
					CustomMenu.Item("Print Color with linenumbers", event -> Print(true, true)),
					CustomMenu.Seperator(),
					CustomMenu.Item("Print Black & White", event -> Print(false, false)),
					CustomMenu.Item("Print Black & White with linenumbers", event -> Print(true, false)),
					CustomMenu.Seperator(),
					CustomMenu.Item("alt Print Color", event -> PrintAlt(false, true)),
					CustomMenu.Item("alt Print Color with linenumbers", event -> PrintAlt(true, true)),
					CustomMenu.Seperator(),
					CustomMenu.Item("alt Print Black & White", event -> PrintAlt(false, false)),
					CustomMenu.Item("alt Print Black & White with linenumbers", event -> PrintAlt(true, false)),
						*/
					CustomMenu.Item("Print", event -> Print(true, true)),
					CustomMenu.Seperator(),
					CustomMenu.Item("alt Print (shows Arduino default print dialog)", event -> PrintAlt(true, true)),
					//CustomMenu.Seperator(),
					//CustomMenu.Item("Settings (not implemented yet)", event -> ShowSettings()),
				});
			cm.Init(true);
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println(thisToolMenuTitle + " could not start!!!");
			return;
		}
	}
	
	private void ShowSettings()
	{
		
	}

	PrintRequestAttributeSet printAset;
	PrintService printService;
	private void Print(boolean _printLineNumbers, boolean _printInColor)
	{
		ShowPrintPreview(_printLineNumbers, _printInColor, (PrintDialogArguments pdArgs) ->  {
			if (pdArgs == null) {
				editor.statusNotice(tr("Printing canceled."));
				return;
			}
			try{
				final MessageFormat header = new MessageFormat(editor.getSketch().getName());
				final MessageFormat footer = new MessageFormat("");
				boolean showPrintDialog = true;
				
				if (printAset == null)
				{
					
					printAset = new HashPrintRequestAttributeSet();
					printAset.add(MediaSizeName.ISO_A4);
				}
				if (printService == null)
				{
					//System.out.println("printService == null");
					PrintService[] printServices = javax.print.PrintServiceLookup.lookupPrintServices(null, null);
					if (printServices.length != 0) printService = printServices[0];
				}
				boolean interactive = true;
			
				if (pdArgs.PaperOrientation == java.awt.print.PageFormat.PORTRAIT)
					printAset.add(OrientationRequested.PORTRAIT);
				else if (pdArgs.PaperOrientation == java.awt.print.PageFormat.LANDSCAPE)
					printAset.add(OrientationRequested.LANDSCAPE);
				else if (pdArgs.PaperOrientation == java.awt.print.PageFormat.REVERSE_LANDSCAPE)
					printAset.add(OrientationRequested.REVERSE_LANDSCAPE);	

				boolean done = pdArgs.jtp.print(header, footer, showPrintDialog, printService, printAset, interactive);
				if (!done) {
					editor.statusNotice(tr("Printing canceled."));
					return;
				}
				editor.statusNotice(tr("Done printing."));
			} catch (PrinterException pe) {
				editor.statusError(tr("Error while printing."));
				pe.printStackTrace();
			}
		});
	}

	private PageFormat pageFormat;
	private void PrintAlt(boolean _printLineNumbers, boolean _printInColor) {

		ShowPrintPreview(_printLineNumbers, _printInColor, (PrintDialogArguments pdArgs) ->  {
			if (pdArgs == null) {
				editor.statusNotice(tr("Printing canceled."));
				return;
			}
			try {
				editor.statusNotice(tr("Printing..."));
				PrinterJob printerJob = PrinterJob.getPrinterJob();
				if (pageFormat == null) pageFormat = printerJob.defaultPage();

				pageFormat.setOrientation(pdArgs.PaperOrientation);
				printerJob.setPrintable(pdArgs.jtp.getPrintable(null, null), pageFormat);

				// set the name of the job to the code name
				printerJob.setJobName(editor.getCurrentTab().getSketchFile().getPrettyName());
			
				if (printerJob.printDialog()) {
				try {
					printerJob.print();
					editor.statusNotice(tr("Done printing."));
				} catch (PrinterException pe) {
					editor.statusError(tr("Error while printing."));
					pe.printStackTrace();
				}
				} else {
					editor.statusNotice(tr("Printing canceled."));
				}
				
			} catch (Exception pex) {
				editor.statusError(tr("Error while printing."));
				pex.printStackTrace();
			}
		});		
	}

	boolean printLineNumbers = false;
	boolean printInColor = false;
	String lineNumberSpacing = "  ";
	PrintDialogArguments pdArgs;
	private void ShowPrintPreview(boolean _printLineNumbers, boolean _printInColor, java.util.function.Consumer<PrintDialogArguments> printButtonPressed)
	{
		if (pdArgs == null) pdArgs = new PrintDialogArguments();
		
		printLineNumbers = _printLineNumbers;
		printInColor = _printInColor;
		JTextPane jtp = new JTextPane();
		pdArgs.jtp = jtp;
		if (_printInColor) jtp.setContentType("text/html");
		else jtp.setContentType("text/text");
		jtp.setFont(editor.getCurrentTab().getTextArea().getFontForTokenType(0));
		jtp.setText(MyDiscourseFormat.GetResult(editor, printInColor, printLineNumbers, lineNumberSpacing));

		javax.swing.JFrame jframe = new javax.swing.JFrame();
        jframe.setSize(750, 600);
		jframe.setDefaultCloseOperation(javax.swing.JFrame.HIDE_ON_CLOSE);
		jframe.addWindowListener(new java.awt.event.WindowAdapter()
        {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e)
            {
				//System.out.println("PrintPreview closing");
                printButtonPressed.accept(null);
                e.getWindow().dispose();
            }
        });

		javax.swing.JButton btn = new javax.swing.JButton("Print");
        btn.addActionListener(new java.awt.event.ActionListener() {

            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
				printButtonPressed.accept(pdArgs);
				//System.out.println("PrintPreview print pressed");
				jframe.setVisible(false);
			}
		});
		javax.swing.JButton btn2 = new javax.swing.JButton("Cancel");
        btn2.addActionListener(new java.awt.event.ActionListener() {

            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
				printButtonPressed.accept(null);
				//System.out.println("PrintPreview cancel pressed");
				jframe.setVisible(false);
			}
		});
		javax.swing.JPanel jpButtons = new javax.swing.JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));
		jpButtons.add(btn);
		jpButtons.add(btn2);
		javax.swing.JPanel jpToolBar = new javax.swing.JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));

		javax.swing.JLabel lblLineNumberSpacing = new javax.swing.JLabel("Line Number Spacing");
		jpToolBar.add(lblLineNumberSpacing);
		javax.swing.JTextArea txtLineNumberSpacing = new javax.swing.JTextArea(lineNumberSpacing);
		txtLineNumberSpacing.setColumns(8);
		jpToolBar.add(txtLineNumberSpacing);
		javax.swing.JButton btnLineNumberSpacing = new javax.swing.JButton("apply");
		jpToolBar.add(btnLineNumberSpacing);
		btnLineNumberSpacing.addActionListener(new java.awt.event.ActionListener() {

            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
				lineNumberSpacing = txtLineNumberSpacing.getText();
				jtp.setText(MyDiscourseFormat.GetResult(editor, printInColor, printLineNumbers, lineNumberSpacing));
			}
		});

		javax.swing.JLabel lblPaperOrientation = new javax.swing.JLabel("Paper Orientation");
		jpToolBar.add(lblPaperOrientation);
		javax.swing.JComboBox cBox = new javax.swing.JComboBox(new String[] {"Landscape", "Portrait", "Reverse Landscape"});
		cBox.setSelectedItem("Portrait");
		cBox.addActionListener(new java.awt.event.ActionListener() {

            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
				if (cBox.getSelectedItem().toString().equals("Portrait"))
					pdArgs.PaperOrientation = java.awt.print.PageFormat.PORTRAIT;
				else if (cBox.getSelectedItem().toString().equals("Landscape"))
					pdArgs.PaperOrientation = java.awt.print.PageFormat.LANDSCAPE;
				else if (cBox.getSelectedItem().toString().equals("Reverse Landscape"))
					pdArgs.PaperOrientation = java.awt.print.PageFormat.REVERSE_LANDSCAPE;
			}
		});
		jpToolBar.add(cBox);

		javax.swing.JCheckBox chkShowLineNumbers = new javax.swing.JCheckBox("Show Line Numbers");
		chkShowLineNumbers.setSelected(printLineNumbers);
		chkShowLineNumbers.addActionListener(new java.awt.event.ActionListener() {

            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
				printLineNumbers = chkShowLineNumbers.isSelected();
				//String lineNumberSpacing = String.format("%1$" + SpacesAfterLineNumber + "s", "");
				jtp.setText(MyDiscourseFormat.GetResult(editor, printInColor, printLineNumbers, lineNumberSpacing));
			}
		});
		jpToolBar.add(chkShowLineNumbers);

		javax.swing.JCheckBox chkShowInColor = new javax.swing.JCheckBox("Show In Color");
		chkShowInColor.setSelected(printInColor);
		chkShowInColor.addActionListener(new java.awt.event.ActionListener() {

            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
				printInColor = chkShowInColor.isSelected();
				//String lineNumberSpacing = String.format("%1$" + SpacesAfterLineNumber + "s", "");
				
				if (printInColor) jtp.setContentType("text/html");
				else jtp.setContentType("text/text");

				jtp.setText(MyDiscourseFormat.GetResult(editor, printInColor, printLineNumbers, lineNumberSpacing));
			}
		});
        jpToolBar.add(chkShowInColor);
		
		
		jframe.add(jpToolBar, java.awt.BorderLayout.NORTH);
		jframe.add(jpButtons, java.awt.BorderLayout.SOUTH);

		javax.swing.JScrollPane scrollableTextArea = new javax.swing.JScrollPane(jtp);  
        scrollableTextArea.setHorizontalScrollBarPolicy(javax.swing.JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);  
		scrollableTextArea.setVerticalScrollBarPolicy(javax.swing.JScrollPane.VERTICAL_SCROLLBAR_ALWAYS); 
		
		jframe.add(scrollableTextArea);
		jframe.setVisible(true);
		jframe.setSize(750, 600);
	}
	class PrintDialogArguments
	{
		public int PaperOrientation = java.awt.print.PageFormat.PORTRAIT;
		public JTextPane jtp;
	}
}
