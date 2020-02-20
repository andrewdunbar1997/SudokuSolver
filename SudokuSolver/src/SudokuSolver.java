import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Scanner;
/**
 * Program to allow the user to enter a Sudoku grid and have it solved
 *
 * @author Andrew Dunbar
 * @version 14 Feb 2020
 */
public class SudokuSolver extends JFrame{
	
	/*
	 * Constants defining the size of the frame
	 */
	private final static int FRAME_WIDTH = 400;
	private final static int FRAME_HEIGHT = 400;
	
	/*
	 * find the resolution of the screen (compatible with multi-monitor set-ups)
	 */
	private static GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
	private final static int SCREEN_WIDTH = gd.getDisplayMode().getWidth();
	private final static int SCREEN_HEIGHT = gd.getDisplayMode().getHeight();
	
	/*
	 * Calculate frame x position and y position to centre the frame on screen
	 */
	private final static int FRAME_X_POS = (SCREEN_WIDTH - FRAME_WIDTH) / 2;
	private final static int FRAME_Y_POS = (SCREEN_HEIGHT - FRAME_HEIGHT) / 2;
	
	/*
	 * Storing the title of the frame
	 */
	private final static String TITLE = "Sudoku Solver";
	
	/*
	 * Grid Properties
	 */
	private final static int GRID_WIDTH = 9;	//number of columns in the sudoku grid
	private final static int GRID_HEIGHT = 9;	//number of rows in the sudoku grid
	private final static int SUBGRID_WIDTH = 3;	//width of the sub-grids
	private final static int SUBGRID_HEIGHT = 3;//height of the sub-grids	
	private final static int MIN_VALUE = 1;		//minimum valid entry in a grid-square
	private final static int MAX_VALUE = 9;		//maximum valid entry in a grid-square
	
	/*
	 * file choosers/ for saving/loading grids
	 */
	private static JFileChooser fileChooser;
	private static FileWriter csvWriter;
	
	/*
	 * variables for storing information relating to the grid and it's solutions
	 */
	private int[][] grid = new int[GRID_HEIGHT][GRID_WIDTH];		//current working grid
	private ArrayList<int[][]> solutions = new ArrayList<int[][]>();//a list of grids. 0 is the unsolved grid
	private static final int MAX_SOLUTIONS = 10;	//maximum number of solutions the program will attempt to calculate;
	private int currentSolution = 0;	//the current solution being displayed by the program
	
	/*
	 * GUI elements
	 */
	private JButton solveButton, clearButton, nextButton, backButton, resetButton, saveButton, loadButton;
	private JTextField[][] inputFields = new JTextField[GRID_HEIGHT][GRID_WIDTH];
	private JTextField solutionNumber;
	private Container inputArea;
	
	/**
	 * The main method. Instantiates the frame and creates the GUI
	 */
	public static void main(String[] args) 
	{		
		//create a new instance of the frame
		SudokuSolver frame = new SudokuSolver();
	}
	/**
	 * Constructor for a new sudoku solver
	 * intialises an instance with default values
	 */
	SudokuSolver()
	{
		this(FRAME_WIDTH, FRAME_HEIGHT, FRAME_X_POS, FRAME_Y_POS, TITLE);
	}
	
	/**
	 * Constructor for a new sudoku solver
	 * 
	 * @param xSize	the width of the frame
	 * @param ySize	the height of the frame
	 * @param xPos	the distance from the left of the screen
	 * @param yPos	the distance from the top of the screen
	 * @param title	the title of the frame
	 */
	SudokuSolver(int xSize, int ySize, int xPos, int yPos, String title)
	{
		setSize(xSize, ySize);	//set the size of the window
		setLocation(xPos, yPos);//set the location of the window on the screen
		setTitle(title);		//give the window a title
		setResizable(false);	//disallow the user from resizing the window
		setDefaultCloseOperation(EXIT_ON_CLOSE);	//make sure the program exits appropriately
		
		//set UI style to conform to windows
		try 
		{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException| UnsupportedLookAndFeelException e) 
		{
			e.printStackTrace();
		}
		
		createGUI();	//use helper method to create the GUI
		
		setVisible(true);	//allow the user to see the GUI
		
		inputFields[0][0].grabFocus();	//set focus to first textbox
	}
	
	/**
	 * Helper method to help create the GUI in the frame.
	 * constructs all buttons, text fields and listeners required
	 */
	private void createGUI()
	{
		//create a container to add the elements to
		Container window = getContentPane();
		//give it a flow layout
		window.setLayout(new FlowLayout());
		
		/*
		 * initialise the solve button
		 */
		solveButton = new JButton("Solve");						//create the button
		solveButton.setFont(new Font("ARIAL", Font.PLAIN, 18));	//set an appropriate font
		solveButton.addActionListener(new SolveListener());		//attach the appropriate listener
		window.add(solveButton);								//add the button to the GUI
		
		/*
		 * initialise the clear button
		 */
		clearButton = new JButton("Clear Grid");				//create the button
		clearButton.setFont(new Font("ARIAL", Font.PLAIN, 18));	//set an appropriate font
		clearButton.addActionListener(new ClearListener());		//attach the appropriate listener
		window.add(clearButton);								//add the button to the GUI
		
		/*
		 * initialise the clear button
		 */
		resetButton = new JButton("Reset");						//create the button
		resetButton.setFont(new Font("ARIAL", Font.PLAIN, 18));	//set an appropriate font
		resetButton.addActionListener(new ResetListener());		//attach the appropriate listener
		resetButton.setEnabled(false);							//disable by default
		window.add(resetButton);								//add the button to the GUI
		
		/*
		 * Create the grid of text fields to allow entering of a grid
		 */
		inputArea = new Container();	//create a new container
		inputArea.setLayout(new GridLayout(GRID_WIDTH, GRID_HEIGHT));	//give it a grid layout of the correct number of rows and columns
		
		for(int row = 0; row < GRID_HEIGHT; row++)				//for row in the grid
		{
			for(int column = 0; column < GRID_WIDTH; column++)	//for each column in the grid
			{
				//set initial border size values
				int topBorder = 1;
				int leftBorder = 1;
				int rightBorder = 1;
				int bottomBorder = 1;
				
				//change border values of at the edge of a grid or sub-grid
				if(column % SUBGRID_WIDTH == 0) leftBorder = 5;
				if(column == GRID_WIDTH - 1) rightBorder = 5;
				if(row % SUBGRID_HEIGHT == 0) topBorder = 5;
				if(row == GRID_HEIGHT - 1) bottomBorder = 5;
				
				//create the required border
				Border border = BorderFactory.createMatteBorder(topBorder, leftBorder, bottomBorder, rightBorder, Color.BLACK);
				
				//initialise the corresponding text field
				inputFields[row][column] = new JTextField(2);						//create text field for 2 characters
				inputFields[row][column].setFont(new Font("ARIAL", Font.BOLD, 20));	//set an appropriate font
				inputFields[row][column].setBackground(Color.WHITE);				//set default background colour
				inputFields[row][column].setForeground(Color.BLACK);				//set default text colour
				inputFields[row][column].setBorder(border);							//apply the constructed border
				inputFields[row][column].setHorizontalAlignment(JTextField.CENTER);	//centres the text
				inputFields[row][column].setText("");								//place an initial value
				
				//add the text field to the container
				inputArea.add(inputFields[row][column]);	
			}
		}
		//add the input grid to the GUI
		window.add(inputArea);
		
		/*
		 * save button for saving a grid to a file
		 * saves the current state of the grid
		 */
		saveButton = new JButton("Save");
		saveButton.setFont(new Font("ARIAL", Font.PLAIN, 18));	//set an appropriate font
		saveButton.addActionListener(new SaveListener());
		window.add(saveButton);
		
		/*
		 * initialise the back button, allows the user to cycle through valid solutions in
		 * combination with the next button
		 */
		backButton = new JButton("<<");						//create the button
		backButton.setFont(new Font("ARIAL", Font.PLAIN, 18));	//set an appropriate font
		backButton.addActionListener(new BackListener());	//attach the appropriate listener
		backButton.setEnabled(false);						//disable button by default
		window.add(backButton);								//add the button to the GUI
		
		/*
		 * text box to hold the number of the solution being displayed
		 */
		solutionNumber = new JTextField(2);							//Initialise the text box
		solutionNumber.setFont(new Font("ARIAL", Font.BOLD, 20));	//set an appropriate font
		solutionNumber.setBackground(Color.WHITE);					//set the background colour
		solutionNumber.setForeground(Color.BLACK);					//set the text colour
		solutionNumber.setText("0");								//place in a default value
		solutionNumber.setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, Color.BLACK));	//apply black border
		solutionNumber.setHorizontalAlignment(JTextField.CENTER);	//centres the text
		solutionNumber.setEditable(false);							//disallow the user from editing this box
		window.add(solutionNumber);									//add to the GUI
		
		/*
		 * initialise the next button, allows the user to cycle through valid solutions in
		 * combination with the back button
		 */
		nextButton = new JButton(">>");						//create the button
		nextButton.setFont(new Font("ARIAL", Font.PLAIN, 18));	//set an appropriate font
		nextButton.addActionListener(new NextListener());	//attach the appropriate listener
		nextButton.setEnabled(false);						//disable button by default
		window.add(nextButton);								//add the button to the GUI
		
		/*
		 * load button for loading a grid from a file
		 */
		loadButton = new JButton("Load");
		loadButton.setFont(new Font("ARIAL", Font.PLAIN, 18));	//set an appropriate font
		loadButton.addActionListener(new LoadListener());
		window.add(loadButton);
	}
	
	/**
	 * method to solve a given sudoku square. adds solutions to the solutions list as it finds them
	 */
	private void solve()
	{
		//if there have been too many solutions already calculated, do nothing and return
		if(solutions.size() > MAX_SOLUTIONS) return;
		
		//loop through each row
		for(int row = 0; row < GRID_HEIGHT; row++)
		{
			//loop through each column
			for(int column = 0; column < GRID_WIDTH; column++)
			{
				//if an unfilled space is found
				if(grid[row][column] == 0)
				{
					//loop through possible values of n
					for(int n = MIN_VALUE; n <= MAX_VALUE; n++)
					{
						//if this value is possible
						if(isPossible(row, column, n))
						{
							//fill in with that value
							grid[row][column] = n;
							
							//solve the rest of the grid
							solve();
							
							//if here, solving has finished, successfully or otherwise
							//return the square to 0
							grid[row][column] = 0;
						}
					}
					return;	//if here, all values of n have been checked, return to function call
				}
			}
		}
		//if here, the end of the grid was reached and all spaces were filled
		//add current state of the grid to the solutions list
		solutions.add(copyGrid(grid));
	}
	
	/**
	 * Method which returns the value of a given grid square. This is done by parsing the
	 * string contained in the given entryField. if successful, just returns the value
	 * if unsuccessful because the value wasn't an integer, checks if the fields was empty
	 * and if so, returns 0. otherwise, returns -1 if the value was invalid
	 * 
	 * @param row		the row of the grid square we are retrieving the value from
	 * @param column	the column of the grid square we are retrieving the value from
	 * 
	 * @return	the value of the string in the text box at the specified row and column. returns 0 of empty and -1 if not an integer or out of range
	 */
	private int getValue(int row, int column)
	{
		try	//try the following code
		{
			//grab the value from the relevant text box
			int value = Integer.parseInt(inputFields[row][column].getText());
			
			//if it's value is inside the range or is 0
			if((value >= MIN_VALUE && value <= MAX_VALUE) || value == 0)
			{
				return value;	//simply return it's value
			}
			else	//otherwise, it is an invalid integer
			{
				return -1;	//return -1 to the method call
			}
		}
		catch(NumberFormatException e)	//if an exception was thrown
		{
			//if the exception was thrown because the field was empty
			if(inputFields[row][column].getText().trim().equals("")) 
			{
				return 0;
			}
			else
			{
				return -1;	//otherwise return -1 for value comparison purposes
			}			
		}
	}
	
	/**
	 * checks if a given number (n) is possible at grid position x,y
	 * a value is possible if that value is not already present in the same row, column or 3x3 sub-square as
	 * the position being checked
	 * 
	 * requires that getGrid be run beforehand
	 * 
	 * @param xPos	the column being checked
	 * @param yPos	the row being checked
	 * @param n		The value being checked for
	 */
	private boolean isPossible(int row, int column, int n)
	{
		for(int i = 0; i < GRID_WIDTH; i++)	//for each grid-square in the same row as the one being checked
		{
			if(i == column) continue;				//skip over the grid-square we are checking against
			if(grid[row][i] == n) return false;//return false if the value has already appeared
		}
		for(int j = 0; j < GRID_HEIGHT; j++)		//repeat for being in the same column
		{
			if(j == row) continue;
			if(grid[j][column] == n) return false;
		}
		
		int x0 = (column/SUBGRID_WIDTH) * SUBGRID_WIDTH;	//determines the first row of the current sub-grid
		int y0 = (row/SUBGRID_HEIGHT) * SUBGRID_HEIGHT;		//determines the first column of the current sub-grid
		
		for(int j = y0; j < y0 + SUBGRID_HEIGHT; j++)	//for each row
		{
			for(int i = x0; i < x0 + SUBGRID_WIDTH; i++)//loop through the rows in the same sub-grid
			{
				if(column == i && row == j) continue;	//skip over the grid-square we are checking against
				if(grid[j][i] == n) return false;		//return false if value found in the same subgrid
			}
		}
		
		return true;	//return true if value wasn't found
	}
	
	/**
	 * this method takes the value of each grid square and stores it in grid
	 */
	private void getGrid()
	{
		for(int row = 0; row < GRID_HEIGHT; row++)
		{
			for(int column = 0; column < GRID_WIDTH; column++)
			{
				grid[row][column] = getValue(row, column);
			}
		}
	}

	/**
	 * Checks if the value in a given grid square is valid, -1 is automatically invalid, 0 is automatically valid
	 * all other integers are checked to see if they are possible
	 * 
	 * @param row		the row of the square being checked
	 * @param column	the column of the square being checked
	 * 
	 * @return			the validity of the grid square
	 */
	private boolean checkSquare(int row, int column)
	{
		if(grid[row][column] == 0) return true;
		if(grid[row][column] == -1) return false;
		return isPossible(row, column, grid[row][column]);
	}

	/**
	 * method to colour a given square in the grid based on if it is:
	 * 	- a blank square	(white with black text)
	 * 	- a clue square 	(dark gray with white text)
	 * 	- an invalid square	(red with white text)
	 * 
	 * @param row		The row of the square to be coloured
	 * @param column	The column of the square to be coloured
	 */
	private void colourSquare(int row, int column)
	{
		if(!checkSquare(row, column))	//if the square is invalid, colour with red background, white text
		{
			inputFields[row][column].setBackground(Color.RED);
			inputFields[row][column].setForeground(Color.WHITE);
		}
		else if(grid[row][column] == 0)	//if the square is blank, colour with white background, black text
		{
			inputFields[row][column].setBackground(Color.WHITE);
			inputFields[row][column].setForeground(Color.BLACK);
		}
		else	//otherwise, the square contains a clue value, colour with dark gray background, white text
		{
			inputFields[row][column].setBackground(Color.DARK_GRAY);
			inputFields[row][column].setForeground(Color.WHITE);
		}
	}

	/**
	 * Method to check if the current displayed grid is valid. checks each grid square
	 * and if any turn out to be invalid, the whole grid is invalid
	 * 
	 * @return	validity of the current grid
	 */
	private boolean checkGrid()
	{
		for(int row = 0; row < GRID_HEIGHT; row++)	//for each row
		{
			for(int column = 0; column < GRID_WIDTH; column++)	//for each column
			{
				if(!checkSquare(row, column)) return false;	//if the given square is invalid, return false
			}
		}
		//if no invalid squares were found, return true
		return true;
	}

	/**
	 * Method to colour all the squares in the grid
	 */
	private void colourGrid()
	{
		//stop the user seeing the input area until after changes are made
		inputArea.setVisible(false);
		
		for(int row = 0; row < GRID_WIDTH; row++)//for each row
		{
			for(int column = 0; column < GRID_HEIGHT; column++)	//for each column
			{
				//colour the square appropriately
				colourSquare(row, column);
			}
		}
		
		//allow the user to see the input area again
		inputArea.setVisible(true);
	}

	/**
	 * method required to make a copy of a 2d array since Arrays.copyOf() only works
	 * for 1D arrays
	 * 
	 * @param inputgrid	the grid to be copied
	 * @return			the copy of the grid
	 */
	private int[][] copyGrid(int[][] inputGrid)
	{
		//create a new grid of the right dimensions
		int[][] copy = new int[GRID_HEIGHT][GRID_WIDTH];
		
		//for each value in the working grid
		for(int i = 0; i < GRID_WIDTH; i++)
		{
			for(int j = 0; j < GRID_HEIGHT; j++)
			{
				//transfer that value to the copy
				copy[j][i] = inputGrid[j][i];
			}
		}
		//return the copy of grid
		return copy;
	}
	
	/**
	 * method to display a given grid in the output field
	 * 
	 * @param inputGrid	the grid to be displayed
	 */
	private void displayGrid(int[][] inputGrid)
	{
		//stop the user from seeing the input area until after changes are made
		inputArea.setVisible(false);
		
		//loop through each row
		for(int i = 0; i < GRID_WIDTH; i++)
		{
			//loop through each row
			for(int j = 0; j < GRID_HEIGHT; j++)
			{
				if(inputGrid[j][i] == 0)			//if the value in the grid was 0;
				{
					inputFields[j][i].setText("");	//set text box to blank
				}
				else
				{
					//enter the value from the grid into it's respective text box
					inputFields[j][i].setText(Integer.toString(inputGrid[j][i]));
				}
			}
		}
		
		//loads the grid into the working memory
		getGrid();
		
		//allow the user to see the input area again
		inputArea.setVisible(true);
	}
	
	/**
	 * method to allow bulk enabling/disabling of all text fields in the entry grid
	 * 
	 * @param enable true/false
	 */
	private void enableGrid(boolean enable)
	{
		//for each text box in the grid
		for(int i = 0; i < GRID_WIDTH; i++)
		{
			for(int j = 0; j < GRID_HEIGHT; j++)
			{
				//enable/disable ability to edit
				inputFields[j][i].setEditable(enable);
			}
		}
	}
	
	/*
	 * All required listener objects for the classes
	 */
	class SolveListener implements ActionListener
	{			
		public void actionPerformed(ActionEvent event)
		{
			getGrid();
			
			//colour the grid
			colourGrid();
			
			//if the grid is invalid, do nothing
			if(!checkGrid()) return;
			
			//add the unsolved grid to the solutions list
			if(solutions.size() == 0)	//if there is nothing in the solutions list
			{
				solutions.add(copyGrid(grid));	//add it as the first item
			}
			else	//otherwise, there is already something in the list
			{
				solutions.set(0, copyGrid(grid));	//set the first item in the list to be the unsolved grid
			}
			
			//take the current grid and solve it
			solve();
			
			//if this produced a solution
			if(solutions.size() > 1)
			{
				displayGrid(solutions.get(1));	//display the first solution
				currentSolution = 1;			//set the current solution counter to 1
				solutionNumber.setText(Integer.toString(currentSolution));	//update the current solution text box
			}
			else //otherwise, the grid was unsolvable
			{
				displayGrid(solutions.get(0));	//simply display the unsolved grid
				currentSolution = 0;			//set the current solution to 0
				solutionNumber.setText(Integer.toString(currentSolution));	//update the current solution text box
			}
			
			//disable the set grid and solve buttons
			solveButton.setEnabled(false);
			enableGrid(false);
			
			//enable the reset button
			resetButton.setEnabled(true);
			
			//enables the back and next buttons
			backButton.setEnabled(true);
			nextButton.setEnabled(true);
		}
	}
	
	/*
	 * Listener for the clear button. this resets the display grid and all internal variables to
	 * default values. also disables the solve button until a new valid starting grid is entered
	 */
	class ClearListener implements ActionListener
	{
		public void actionPerformed(ActionEvent event)
		{
			//reinitialises the solutions list as an empty list
			solutions = new ArrayList<int[][]>();
			
			//reinitialises the grid as a grid of zeros
			grid = new int[GRID_HEIGHT][GRID_WIDTH];
			
			//displays the new empty grid
			displayGrid(grid);
			
			//recolours the grid
			colourGrid();
			
			//re-enable the solve button
			solveButton.setEnabled(true);
			
			//disables the reset button to since unsolved grid has been cleared
			resetButton.setEnabled(false);
			
			//enables the set grid button to allow the user to enter a grid
			enableGrid(true);
			
			//sets the current solution to 0 and updates the appropriate text box
			currentSolution = 0;
			solutionNumber.setText(Integer.toString(currentSolution));
			
			//disables the back and next buttons
			backButton.setEnabled(false);
			nextButton.setEnabled(false);
			
			//set focus to the first text box
			inputFields[0][0].grabFocus();
		}
	}
	
	class ResetListener implements ActionListener
	{
		public void actionPerformed(ActionEvent event)
		{
			//display the unsolved grid
			displayGrid(solutions.get(0));
			
			//reinitialises the solutions list as an empty list
			solutions = new ArrayList<int[][]>();
			
			//reinitialises the grid as a grid of zeros
			grid = new int[GRID_HEIGHT][GRID_WIDTH];

			//re-enable the solve button
			solveButton.setEnabled(true);
			
			//disables the reset button
			resetButton.setEnabled(false);
			
			//enables the set grid button to allow the user to enter a grid
			enableGrid(true);
			
			//sets the current solution to 0 and updates the appropriate text box
			currentSolution = 0;
			solutionNumber.setText(Integer.toString(currentSolution));
			
			//disables the back and next buttons
			backButton.setEnabled(false);
			nextButton.setEnabled(false);
			
			//set focus to the first text box
			inputFields[0][0].grabFocus();
		}
	}
	
	/*
	 * listener for the back button, displays the previous solution
	 */
	class BackListener implements ActionListener
	{
		public void actionPerformed(ActionEvent event)
		{
			//if we aren't already on solution 0
			if(currentSolution > 0)
			{
				//decrease curentSolution by 1
				currentSolution--;
				//display the new grid
				displayGrid(solutions.get(currentSolution));
				//update the solution text box
				solutionNumber.setText(Integer.toString(currentSolution));
			}
		}
	}
	
	/*
	 * listener for the next button. displays the next solution
	 */
	class NextListener implements ActionListener
	{
		public void actionPerformed(ActionEvent event)
		{
			//if we aren't already at the last solution
			if(currentSolution < solutions.size()-1)
			{
				//add 1 to the currentSolution
				currentSolution++;
				//display the new solution
				displayGrid(solutions.get(currentSolution));
				//update the solution text box
				solutionNumber.setText(Integer.toString(currentSolution));
			}
		}
	}
	
	/*
	 * listener for the save button
	 */
	class SaveListener implements ActionListener
	{
		public void actionPerformed(ActionEvent event)
		{
			//load the current grid to memory
			getGrid();
			
			//if there is no grid currently in memory, color the grid
			//this prevents the save button colouring in a solved grid
			if(currentSolution == 0 && solutions.size() == 0)
			{
				colourGrid();
			}
			
			if(!checkGrid()) return;	//if grid is invalid, do nothing
			
			/*
			 * initialise the file chooser
			 */
			fileChooser = new JFileChooser();
			//set directory to default
			fileChooser.setCurrentDirectory(null);
			//set file chooser title
			fileChooser.setDialogTitle("Save");
			//disallow the user from selecting "all files"
			fileChooser.setAcceptAllFileFilterUsed(false);
			//set a file filter for CSV files
			fileChooser.setFileFilter(new FileNameExtensionFilter("Comma Seperated Value Files", "csv"));
			
			//if a file path was selected
			if(fileChooser.showSaveDialog(saveButton) != JFileChooser.APPROVE_OPTION) return;
			//store that file path as a string
			String filePath = "" + fileChooser.getSelectedFile();
			
			//check if it ends in .csv
			if(!filePath.substring(filePath.length()-4, filePath.length()).equals(".csv"))
			{
				//if it doesn't, add the extension
				filePath = filePath + ".csv";
			}
			
			//create the file at the file path
			File fileToSave = new File(filePath);
			
			try //try the following code
			{
				//create a new file writer
				csvWriter = new FileWriter(fileToSave);
				
				//if the file already existed, overwrite the existing file
				csvWriter.write("");
				
				//for each row
				for(int j = 0; j < GRID_HEIGHT; j++)
				{
					//for each column
					for(int i = 0; i < GRID_WIDTH; i++)
					{
						//append the current grid-squares value to the file
						csvWriter.append(Integer.toString(grid[j][i]));
						
						if(i < GRID_WIDTH - 1)	//if not the last value in the row
						{
							csvWriter.append(",");	//separate with a comma
						}
						else	//otherwise, is the last value in the row
						{
							csvWriter.append("\n");	//add a new line character
						}
					}
				}
				
				//flush and close the file writer
				csvWriter.flush();
				csvWriter.close();
			} 
			catch (IOException e) //if an exception was thrown
			{
				JOptionPane.showMessageDialog(null, "An unexpected IO error occurred");
			}				
		}
	}
	
	/*
	 * listener for the load button
	 * clears the grid then enters values from a file
	 */
	class LoadListener implements ActionListener
	{
		public void actionPerformed(ActionEvent event)
		{
			//create a list of strings to hold each individual line
			ArrayList<String> lines = new ArrayList<String>();
			
			//create a flag to hold if the file is valid for current configuration
			boolean validFile = true;
			
			//create an integer grid to hold the values
			int[][] fileGrid = new int[GRID_HEIGHT][GRID_WIDTH];
			
			//clear the program
			clearButton.doClick();
			
			//initialise the file chooser
			fileChooser = new JFileChooser();
			//set it's default directory
			fileChooser.setCurrentDirectory(null);
			//give the file chooser a title
			fileChooser.setDialogTitle("Load");
			//disallow the user from selecting "all files"
			fileChooser.setAcceptAllFileFilterUsed(false);
			//filter for CSV files only
			fileChooser.setFileFilter(new FileNameExtensionFilter("Comma Seperated Value Files", "csv"));				
			
			//if a file was chosen
			if(fileChooser.showOpenDialog(loadButton) == JFileChooser.APPROVE_OPTION)
			{
				//grab the file
				File fileToLoad = fileChooser.getSelectedFile();
				
				try 	//try the following code
				{
					//create a scanner to read the file
					Scanner fileScanner = new Scanner(fileToLoad);
					
					while(fileScanner.hasNextLine())	//while there is another line to read
					{
						//store the line in the list
						lines.add(fileScanner.nextLine());
					}
					
					//if there are more/less lines in the file than rows in the grid
					//file is invalid
					if(lines.size() != GRID_HEIGHT) validFile = false;
					
					//for each line read
					for(int i = 0; i < GRID_HEIGHT; i++)
					{
						//create an integer to count commas (delimiter in csv files)
						int commaCount = 0;
						
						//for each character in the string
						for(int j = 0; j < lines.get(i).length(); j++)
						{
							//check if it is a comma and if so, increment the comma count
							if(lines.get(i).charAt(j) == ',') commaCount++;
						}
						
						//if there are more commas than expected, the file is invalid
						if(commaCount != GRID_WIDTH - 1) validFile = false;
					}
					
					//close the file scanner
					fileScanner.close();
					
					if(validFile)	//if the file was in a valid format
					{
						boolean errorFound = false;	//flag to track format errors
						
						for(int j = 0; j < GRID_HEIGHT; j++)	//for each line
						{
							//create a scanner for the line
							Scanner lineScanner = new Scanner(lines.get(j));
							//set it up to use commas as a delimiter
							lineScanner.useDelimiter(",");
							
							//for each text box in the row
							for(int i = 0; i < GRID_WIDTH; i++)
							{
								if(lineScanner.hasNextInt())	//if there is an integer in the next element
								{
									fileGrid[j][i] = lineScanner.nextInt();	//store that in the file grid
								}
								else
								{
									errorFound = true;
									JOptionPane.showMessageDialog(null, "Invalid file. File contained an invalid entry in row " + j + " and column " + i);
									break;
								}
							}
							//close the line scanner
							lineScanner.close();
							
							//if there were errors
							if(errorFound)
							{
								return; //do nothing
							}
							else
							{
								//display the grid
								displayGrid(fileGrid);
								//colour the grid appropriately
								colourGrid();
								
								//set focus to the first text box
								inputFields[0][0].grabFocus();
							}
						}
					}
					else	//file was not valid
					{
						//alert the user
						JOptionPane.showMessageDialog(null, "Invalid file. File contained incorrect number of data entries.");
					}						
				} 
				catch (FileNotFoundException e) //if the file couldn't be found
				{
					//alert the user
					JOptionPane.showMessageDialog(null, "File not Found");
				}
			}
		}
	}
}