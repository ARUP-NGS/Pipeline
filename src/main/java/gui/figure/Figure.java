/********************************************************************
*
* 	Copyright 2011 Brendan O'Fallon
*
*   Licensed under the Apache License, Version 2.0 (the "License");
*   you may not use this file except in compliance with the License.
*   You may obtain a copy of the License at
*
*       http://www.apache.org/licenses/LICENSE-2.0
*
*   Unless required by applicable law or agreed to in writing, software
*   distributed under the License is distributed on an "AS IS" BASIS,
*   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*   See the License for the specific language governing permissions and
*   limitations under the License.
*
***********************************************************************/


package gui.figure;

import gui.figure.series.AxesElement;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import javax.swing.JPanel;
import javax.swing.Timer;


/**
 * Figure is the superclass of objects that use custom painting, that can be stretched to fill a 
 * component of almost any size, and whose painted elements listen to and respond to mouse events
 * such as clicks and drags.
 *   The Figure itself doesn't do any drawing, but instead calls .paint for each of a maintained list
 * of FigureElements. Each FigureElement knows how to paint itself, and different types of FigureElements
 * look different (for instance text, chart axes, annotation boxes, etc). 
 *   Most elements in a figure know nothing of pixels, only their size relative to their component size.
 * In order to do the actual drawing however, they need to know how big their containing Figure is, so
 * after resizing setScale is called for each component elements, which has argument of the current height
 * width, and the current graphics object. 
 *  This class also listens for mouse clicks, and if a click is within a figure elements boundaries, the
 * element is informed that it has been clicked (or double clicked).  
 * @author brendan
 *
 */
public class Figure extends JPanel implements ComponentListener, KeyListener {

	int minWidth = 100;
	int minHeight = 100;
	int currentX = 0;
	int currentY = 0;
	java.awt.Point mousePos;	//The mouse position in java.awt.coordinate (integer)terms
	Point2D.Double mousePosFigure; //The mouse position in figure (0..1) terms
	protected FigureMouseListener mouseListener;
	
	//When true, painting begins with drawing a white rectangle over everything.
	//Not every Figure type may want this (treeFigures don't), so it's possible to
	//turn this off
	protected boolean autoClear = true;

	java.awt.Point dragStart = null;
	
	protected Color backgroundColor = Color.white;
	
	boolean useAntialiasing = true;
	
	//ArrayList of objects that a) draw themselves and b) have locations
	protected ElementList elements;
	
	protected ArrayList<FigureElement> mouseListeningElements; //Those elements to notify of mouse motion events
	
	private boolean rectangleSelection = false; //On if dragging the mouse creates a selection rectangle
	private boolean currentlyRectSelecting = true; //Turns on drag selection box
	private boolean preserveSelectionRect = false; //Turns on whether selection rectangle is preserved after dragging
	private boolean selectionRectIsPreserved = false; //True if the selection rect is in preserved state
	protected Rectangle selectRect; //The current mouse drag induced selection rectangle
	protected Rectangle prevRect;
	protected Color highlightSquareColor = new Color(0.3f, 0.5f, 0.8f, 0.1f);
	protected boolean mouseDrag = false;
	protected Dimension mouseBegin = new Dimension(0, 0);
	protected Dimension mouseEnd = new Dimension(0, 0);
	
	//Whether or not to use the fancy timing mechanism that allows multiple elements to be double-clicked
	boolean useDoubleClickTimer = false;
	
	javax.swing.Timer clickTimer;
	MouseClickTimer timerListener;
	
	public Figure() {
		setMinimumSize(new Dimension(minWidth, minHeight));
		elements = new ElementList();
		mouseListener = new FigureMouseListener();
		this.setFocusable(true);
		addMouseListener(mouseListener);
		addMouseMotionListener(mouseListener);
		addComponentListener(this);
		addKeyListener(this);
		this.setFocusTraversalKeysEnabled(false);
		mouseListeningElements = new ArrayList<FigureElement>();
		mousePosFigure = new Point2D.Double(0,0);
		selectRect = new Rectangle(0, 0, 0, 0);
		prevRect = new Rectangle(0, 0, 0, 0);
		if (clickTimer == null) {
			timerListener = new MouseClickTimer();
			clickTimer = new Timer(200, timerListener);
			clickTimer.setInitialDelay(200);
		}
	}	
	
	/**
	 * Get the background color (fill color) of this figure
	 */
	public Color getBackground() {
		return backgroundColor;
	}
	
	/**
	 * Set the background color of this figure
	 */
	public void setBackground(Color backgroundColor) {
		this.backgroundColor = backgroundColor;
		repaint();
	}
	
	/**
	 * Add a new element to the list of FigureELements
	 * @param ce
	 */
	public void addElement(FigureElement element) {
		elements.add(element);
	}
	
	public void removeElement(FigureElement el) {
		elements.remove(el);	
	}
	
	/**
	 * Remove all elements from this figure. This typically means that this
	 * Figure will no longer draw anything. 
	 */
	public void removeAllElements() {
		elements.removeAll();
	}
	
	public void addMouseListeningElement(FigureElement el) {
		mouseListeningElements.add(el);
	}
	
	public void removeMouseListeningElement(FigureElement el) {
		mouseListeningElements.remove(el);
	}
	
	/**
	 * Some Figures may wish to allow double-clicks on multiple selected elements; passing in 
	 * true here turns this capability on. Doing this requires a brief pause to count clicks 
	 * before elements become unselected and is likely not 
	 * desireable for most Figures. 
	 * @param allow
	 */
	public void setAllowClicksOnMultipleSelection(boolean allow) {
		this.useDoubleClickTimer = allow;
	}
	
	public boolean isRectangleSelection() {
		return rectangleSelection;
	}
	
	/**
	 * Returns true if there's currently a selection rectangle (either preserved or currently being 
	 * dragged) 
	 */
	public boolean hasSelectionRect() {
		return selectionRectIsPreserved || currentlyRectSelecting;
	}
	
	/**
	 * Set whether mouse dragging draws a selection rectangle
	 * @param rectangleSelection
	 */
	public void setRectangleSelection(boolean rectangleSelection) {
		this.rectangleSelection = rectangleSelection;
	}

	/**
	 * Set whether to preserve the selection rectangle after the user stops dragging (until the next mouse click)
	 * @return
	 */
	public boolean isPreserveSelectionRect() {
		return preserveSelectionRect;
	}

	public void setPreserveSelectionRect(boolean preserveSelectionRect) {
		this.preserveSelectionRect = preserveSelectionRect;
	}
	
	/**
	 * Shift all units by dx in the x direction
	 * @param dx
	 */
	protected void shiftAllX(double dx) {
		for(Object el : elements) {
			((FigureElement)el).shiftX(dx); 
		}
	}
	
	/**
	 * Shift all elements by dy units in the y direction
	 * @param dy
	 */
	protected void shiftAllY(double dy) {
		for(Object el : elements) {
			((FigureElement)el).shiftY(dy); 
		}
	}
	
	/**
	 * Multiply all positions and bounds by the specified x and y amount 
	 */
	protected void rescaleAll(double x, double y) {
		for(Object el : elements) {
			FigureElement figEl = (FigureElement)el; 
			figEl.rescalePosition(x, y);
			figEl.rescaleSize(x, y);
		}
	}
	
	/**
	 * Multiply all positions by the specified x and y amount 
	 */
	protected void rescalePositions(double x, double y) {
		for(Object el : elements) {
			FigureElement figEl = (FigureElement)el; 
			figEl.rescalePosition(x, y);
		}
	}
	
	/**
	 * Multiply all sizes by the specified x and y amount 
	 */
	protected void rescaleAllSizes(double x, double y) {
		for(Object el : elements) {
			FigureElement figEl = (FigureElement)el; 
			figEl.rescaleSize(x, y);
		}
	}
	
	/**
	 * Generate a saveable copy of the image of the Figure
	 * @return
	 */
	public BufferedImage getImage() {
		Image img = this.createImage(getWidth(), getHeight());
		BufferedImage bi;
		if (img instanceof BufferedImage) {
			bi = (BufferedImage)img;
			Graphics graphics = bi.getGraphics();
			paintComponent(graphics);
			return bi;
		}
		else {
			System.out.println("Uh-oh, createImage didn't return a buffered image... dang \n");
			return null;
		}

	}
	
	/**
	 * Turn on / off rectangle selection... this is pretty kludgy and I believe is only used by TreeFigure
	 * to disable rectangle selection whilst dragging scale objects?
	 * @param sel
	 */
	protected void setCurrentlyRectSelecting(boolean sel) {
		currentlyRectSelecting = sel;
		selectionRectIsPreserved = false;
	}
	
	public void requestFocus() {
		this.requestFocusInWindow();
	}
	
	/**
	 * The number of currently selected elements (delegates to ElementList)
	 * @return
	 */
	public int getSelectedElementCount() {
		return elements.getNumSelectedElements();
	}
	
	/**
	 * Returns true if at least one element is selected
	 * @return
	 */
	public boolean somethingIsSelected() {
		return elements.somethingIsSelected();
	}
	
	/**
	 * Returns the topmost element whose bounds contain the point pos, pos is assumed to
	 * be in PIXEL coordinates, or null if no such element exists. 
	 * 
	 * @param pos
	 * @return The Element clicked on or null
	 */
	public FigureElement getElementForPosition(java.awt.Point pos) {
		double clickX = (double)pos.getX()/(double)getWidth();
		double clickY = (double)pos.getY()/(double)getHeight();
		//System.out.println("Click pos x: " + clickX + " click pos y: " + clickY);

		//Traverse in reverse order so elements that are on top get called first
		for(int i=elements.size()-1; i>=0; i--) {
			FigureElement element = elements.get(i);
			if (element.contains(clickX, clickY)) {
				return element;
			}
		}
		return null;
	}
	
	/**
	 * Called during mouse drag events that change the selection rectangle, if rectangleSelection is on.
	 * Subclasses that set rectangleSelection to true should override this to do something
	 * @param selectRect2 The rectangle containing the selected area, in *pixels*
	 */
	public void selectionRectUpdated(Rectangle selectRect2) {	}
	
	/**
	 * Calls .paint on each FigureElement
	 */
	public void paintComponent(Graphics g) {
		Shape origClip = g.getClip();
		Graphics2D g2d = (Graphics2D)g;
		boolean rescale = (currentX != getWidth() || currentY != getHeight());
		
		System.out.println("Paintin figure, width : " + getWidth() + " height: " + getHeight() + " currentX: " + currentX + " currentY: " + currentY);
		rescale = true;
		
		if (autoClear) {
			g2d.setColor(backgroundColor);
			g2d.fillRect(0, 0, getWidth()-1, getHeight()-1);
		}
		
		
		if (rectangleSelection && mouseDrag && isCurrentlyRectSelecting()) {
			g2d.setColor(Color.white);
			g2d.fill(prevRect);
		}
		
		
		
		if (useAntialiasing) {
			g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
	                RenderingHints.VALUE_ANTIALIAS_ON);
		}

		if ((rectangleSelection && mouseDrag && isCurrentlyRectSelecting()) || isSelectionRectIsPreserved()) {
			g2d.setColor(highlightSquareColor);
			g2d.fill(selectRect);
			g2d.setColor(Color.DARK_GRAY);
			g2d.drawRect(selectRect.x, selectRect.y, selectRect.width-1, selectRect.height-1);
			prevRect.x = selectRect.x;
			prevRect.y = selectRect.y;
			prevRect.width = selectRect.width;
			prevRect.height = selectRect.height;				
		}
		
		//Make sure that all elements get rescale called before paint
		if (rescale) {
			for(Object element : elements) {
				((FigureElement) element).setScale(getWidth(), getHeight(), g2d);		
			}
		}
		
		for(Object element : elements) {
			((FigureElement) element).paint(g2d);
		}
		
		
		g.setClip(origClip);

	}
	
	private void selectNextItem() {
		int i = 0;
		boolean selectNext = true;
		while(i<elements.size() && (! elements.get(i).isSelected()) ) {
			i++;
		}
		if (i<elements.size()) {
			if (elements.get(i) instanceof AxesElement && ((AxesElement)elements.get(i)).isXSelected()) {
					((AxesElement)elements.get(i)).setSelected(true);
					selectNext = false;
			}
			else
				elements.get(i).setSelected(false);
		}
		
		if (i==elements.size()) {
			i=0;
		}

		if (selectNext)
			elements.get((i+1)%elements.size()).setSelected(true);
		repaint();
	}
	
	
	public void componentHidden(ComponentEvent arg0) { }


	public void componentMoved(ComponentEvent arg0) { }

	public void componentResized(ComponentEvent arg0) {
		//System.out.println("Figure has been resized, rescaling all elements to X: " + getWidth() + " y: " + getHeight());
		currentX = getWidth();
		currentY = getHeight();
		for(Object element : elements) {
			((FigureElement) element).setScale(currentX, currentY, this.getGraphics());
		}
	}


	public void componentShown(ComponentEvent arg0) { }
	
	/**
	 * Called 0.25 seconds after the user has clicked at least once in the region. This is so
	 * we can handle single vs. double clicks separately. Otherwise, when the user double clicks,
	 * a single and then a double click event are fired. This means the user can never double
	 * click on multiply selected elements (since the first click we deselect them all, except
	 * possibly one). 
	 * @param clicks
	 * @param pos
	 */
	public void handleMouseClickEvent(int clicks, Point pos) {
		double clickX = (double)pos.getX()/(double)getWidth();
		double clickY = (double)pos.getY()/(double)getHeight();

		//System.out.println("Handling mouse click event with # clicks = " + clicks);
		//Kind of confusing implementation, the tricky part is that we want to call
		//unclicked() even if the click has been consumed. 
		boolean clickConsumed = false;  
		
		if (clicks>1) {
			//Traverse in reverse order so elements that are on top get called first
			for(int i=elements.size()-1; i>=0; i--) {
				FigureElement element = elements.get(i);
				if (element.contains(clickX, clickY)) {
					if (!clickConsumed) {
						element.doubleClicked(pos);
						clickConsumed = element.consumesMouseClick();
					}
				}
				
				
			}
		}
		else {	//Single click
			boolean somethingClicked = false;
			for(int i=elements.size()-1; i>=0; i--) {
				FigureElement element = elements.get(i);
				if (element.contains(clickX, clickY)) {
					if (!clickConsumed) {  
						element.clicked(pos);
						somethingClicked = true;
						clickConsumed = element.consumesMouseClick();		
					}
				}
				else { 
					element.unClicked();
				}		
			}
			
			if (! somethingClicked) {
				selectRect.x = 0;
				selectRect.y = 0;
				selectRect.width = 0;
				selectRect.height = 0;
				selectionRectUpdated(selectRect);
			}
			
		}// Single click
		
		repaint();		
	}
	
	class MouseClickTimer implements ActionListener {
		int clicks = 0;
		Point pos;
		
		public void incrementClickCount() {
			clicks++;
		}
		
		public void setPosition(Point pos) {
			this.pos = pos;
		}
	
		public void actionPerformed(ActionEvent arg0) {
			//System.out.println("Stopping timer, tl clicks: " + clicks);
			clickTimer.stop();
			handleMouseClickEvent(clicks, pos);
			clicks = 0;
		}
	}
	
	class FigureMouseListener implements MouseListener, MouseMotionListener {

		/**
		 * When the mouse is clicked we select whatever elements the mouse click was on, and then
		 * we start a timer that lasts for 0.25 seconds to see if subsequent clicks occur. When the timer
		 * is running we just count the number of additional clicks. When the timer expires handleMouseClick() is
		 * called with the total number of clicks that occurred. 
		 * 
		 *  This implementation means that we wait for a brief period of time before things become 'unclicked', which
		 *  can be slightly annoying, but I think it has to be this way if we want the user to be about to double-click
		 *  on multiple selected elements. 
		 * 
		 */
		public void mouseClicked(MouseEvent evt) {
			requestFocus();
			
			if (useDoubleClickTimer) {
				if (! clickTimer.isRunning() ) {
					Point pos = evt.getPoint();
					double clickX = (double)pos.getX()/(double)getWidth();
					double clickY = (double)pos.getY()/(double)getHeight();
					boolean clickConsumed = false;  

					for(int i=elements.size()-1; i>=0 && !clickConsumed; i--) {
						FigureElement element = elements.get(i);
						if (element.contains(clickX, clickY)) {
							if (!clickConsumed) {  
								element.clicked(pos);
								clickConsumed = element.consumesMouseClick();		
							}
						}
					}

					timerListener.incrementClickCount();
					timerListener.setPosition(pos);
					clickTimer.start();
				}
				else {
					timerListener.incrementClickCount();
					timerListener.setPosition(evt.getPoint());
				}
			}
			else {
				handleMouseClickEvent(evt.getClickCount(), evt.getPoint());
			}
			
			setSelectionRectIsPreserved(false);
		}

		public void mouseEntered(MouseEvent arg0) {
			
		}

		public void mouseExited(MouseEvent arg0) {
			
		}

		/**
		 * When the mouse is pressed it may be the beginning of a mouse drag, so we record
		 * the location of the mouse, and we also notify all mouseListeningElements that the
		 * mouse was pressed
		 */
		public void mousePressed(MouseEvent arg0) {
			mouseDrag = true;
			dragStart = new java.awt.Point(arg0.getX(), arg0.getY());
			mouseBegin.width = arg0.getX();
			mouseBegin.height = arg0.getY();
			
			mousePosFigure.x = arg0.getX()/(double)getWidth();
			mousePosFigure.y = arg0.getY()/(double)getHeight();
			for(FigureElement el : mouseListeningElements) {
				el.mousePressed(mousePosFigure);
			}
		
			if (mouseListeningElements.size()>0)
				repaint();
		}

		public void mouseReleased(MouseEvent arg0) {
			mouseDrag = false;
			dragStart = null;
			
			mousePosFigure.x = arg0.getX()/(double)getWidth();
			mousePosFigure.y = arg0.getY()/(double)getHeight();
			for(FigureElement el : mouseListeningElements) {
				el.mouseReleased(mousePosFigure);
			}
			
			currentlyRectSelecting =false; 
			if (preserveSelectionRect)
				setSelectionRectIsPreserved(true);
			
			repaint();
		}

		public void mouseDragged(MouseEvent arg0) {
			boolean moved = false;
			if (dragStart != null && !isCurrentlyRectSelecting()) {
				double deltaX = (arg0.getX()-dragStart.getX())/getWidth();
				double deltaY = (arg0.getY()-dragStart.getY())/getHeight();

				for(Object el : elements) {
					FigureElement element = (FigureElement)el;
					if (element.isSelected() && element.isMobile()) {
						element.move(deltaX, deltaY);
						moved = true;
					}
				}
			}
			
			//If something was moved, repaint. 
			if (moved)
				repaint();
			
			if (rectangleSelection && !moved) {
				currentlyRectSelecting = true;
				mouseEnd.width = arg0.getX();
				mouseEnd.height = arg0.getY();
				int fromX;
				int distX;
				if (mouseBegin.width > mouseEnd.width){
					fromX = mouseEnd.width;
					distX = mouseBegin.width - mouseEnd.width;
				}
				else {
					fromX = mouseBegin.width;
					distX = mouseEnd.width - mouseBegin.width;
				}
				
				int fromY; 
				int distY; 
				if (mouseBegin.height > mouseEnd.height){
					fromY = mouseEnd.height;
					distY = mouseBegin.height - mouseEnd.height;
				}
				else {
					fromY = mouseBegin.height;
					distY = mouseEnd.height - mouseBegin.height;
				}
				
				selectRect.x = fromX;
				selectRect.y = fromY;
				selectRect.width = distX;
				selectRect.height = distY;
				
				selectionRectUpdated(selectRect);
				repaint();
			}
			
			dragStart.x = arg0.getX();
			dragStart.y = arg0.getY();
			
			//The current policy is that if a FigureElement is being moved, we DO NOT call mouseDragged on the other
			//elements; the reasoning being that we don't want a bunch of other things to happen (like selection regions)
			//to be drawn if an element is being dragged around. This may change. 
			if (!moved) {
				mousePosFigure.x = arg0.getX()/(double)getWidth();
				mousePosFigure.y = arg0.getY()/(double)getHeight();
				for(FigureElement el : mouseListeningElements) {
					el.mouseDragged(mousePosFigure);
				}
				if (mouseListeningElements.size()>0)
					repaint();
			}
		}


		/**
		 * We tell all figure elements in mouseListeningElements what the mouse position is
		 * in FIGURE (0..1) terms
		 */
		public void mouseMoved(MouseEvent arg0) {
			mousePos = arg0.getPoint();
			mousePosFigure.x = mousePos.x/(double)getWidth();
			mousePosFigure.y = mousePos.y/(double)getHeight();
			
			for(FigureElement el : mouseListeningElements) {
				el.mouseMoved(mousePosFigure);
			}
			
			if (mouseListeningElements.size()>0)
				repaint();
		}

	}

	
	/****  KeyListener implementation *********/
	
	public void keyPressed(KeyEvent ke) {
		if (ke.getKeyChar()=='\t') {
			selectNextItem();
		}
	}

	public void keyReleased(KeyEvent arg0) {		
	}


	public void keyTyped(KeyEvent arg0) {		
	}

	public boolean isCurrentlyRectSelecting() {
		return currentlyRectSelecting;
	}

	public void setSelectionRectIsPreserved(boolean selectionRectIsPreserved) {
		this.selectionRectIsPreserved = selectionRectIsPreserved;
	}

	public boolean isSelectionRectIsPreserved() {
		return selectionRectIsPreserved;
	}
	
	public void clearSelection() {
		currentlyRectSelecting = false;
		selectionRectIsPreserved = false;
		selectRect.x = 0;
		selectRect.y = 0;
		selectRect.width = 0;
		selectRect.height = 0;
	}

	
}
