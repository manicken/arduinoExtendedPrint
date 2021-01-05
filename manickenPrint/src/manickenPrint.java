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
	PrintRequestAttributeSet printAset;
	PrintService printService;
	private void Print(boolean printLineNumbers, boolean printInColor)
	{
		JTextPane jtp = new JTextPane();
		if (printInColor)
			jtp.setContentType("text/html");
		else
			jtp.setContentType("text/text");

		String lineNumberSpacing = String.format("%1$" + SpacesAfterLineNumber + "s", "");

		jtp.setText(MyDiscourseFormat.GetResult(editor, printInColor, printLineNumbers, lineNumberSpacing));
		jtp.setFont(editor.getCurrentTab().getTextArea().getFontForTokenType(0));
		try{
			final MessageFormat header = new MessageFormat(editor.getSketch().getName());
			final MessageFormat footer = new MessageFormat("");
			boolean showPrintDialog = true;
			
			if (printAset == null)
			{
				
				printAset = new HashPrintRequestAttributeSet();
				printAset.add(MediaSizeName.ISO_A4);
				//printAset.add(new PrinterResolution(300, 300, PrinterResolution.DPI));
				//printAset.add(new MediaPrintableArea(2, 2, 210 - 4, 297 - 4, MediaPrintableArea.MM));
			}
			if (printService == null)
			{
				System.out.println("printService == null");
				PrintService[] printServices = javax.print.PrintServiceLookup.lookupPrintServices(null, null);
				if (printServices.length != 0) printService = printServices[0];
			}
			boolean interactive = true;
			jtp.print(header, footer, showPrintDialog, printService, printAset, interactive);
		}
		catch (Exception ex) {ex.printStackTrace();}
	}

	/**
	 * This is the code that runs after the Arduino IDE GUI has been loaded
	 */
	private void init() {
		try{
			CustomMenu cm = new CustomMenu(this.editor, thisToolMenuTitle, 
				new JMenuItem[] {
					CustomMenu.Item("Print Color", event -> Print(false, true)),
					CustomMenu.Item("Print Color with linenumbers", event -> Print(true, true)),
					CustomMenu.Seperator(),
					CustomMenu.Item("Print Black & White", event -> Print(false, false)),
					CustomMenu.Item("Print Black & White with linenumbers", event -> Print(true, false)),
					CustomMenu.Seperator(),
					CustomMenu.Item("alt Print Color", event -> handlePrint(false, true)),
					CustomMenu.Item("alt Print Color with linenumbers", event -> handlePrint(true, true)),
					CustomMenu.Seperator(),
					CustomMenu.Item("alt Print Black & White", event -> handlePrint(false, false)),
					CustomMenu.Item("alt Print Black & White with linenumbers", event -> handlePrint(true, false)),
					CustomMenu.Seperator(),
					CustomMenu.Item("Settings (not implemented yet)", event -> ShowSettings()),
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
	private PageFormat pageFormat;
	private void handlePrint(boolean printLineNumbers, boolean printInColor) {
		JTextPane jtp = new JTextPane();
		if (printInColor)
			jtp.setContentType("text/html");
		else
			jtp.setContentType("text/text");

		String lineNumberSpacing = String.format("%1$" + SpacesAfterLineNumber + "s", "");

		jtp.setText(MyDiscourseFormat.GetResult(editor, printInColor, printLineNumbers, lineNumberSpacing));
		jtp.setFont(editor.getCurrentTab().getTextArea().getFontForTokenType(0));

		editor.statusNotice(tr("Printing..."));
		//printerJob = null;
		PrinterJob printerJob = PrinterJob.getPrinterJob();
		if (pageFormat == null) pageFormat = printerJob.defaultPage();
		pageFormat.setOrientation(0);
		printerJob.setPrintable(jtp.getPrintable(null, null), pageFormat);

		//}
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
		//printerJob = null;  // clear this out?
	  }
}
