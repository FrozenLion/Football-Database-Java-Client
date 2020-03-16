import java.awt.*;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.Math;

import javax.swing.*;

public class DBWindow extends JFrame{
	public static final int MAX_ATTR = 6;
	public static final int PADDING = 4;
	public static final String[] TABLES = {"Player","in_year", "Team", "Game"};
	public static final String[] CONFERENCES = {"Atlantic Coast Conference", "Big 12 Conference", "Big East Conference", "Big Ten Conference", "Conference USA","Independent","Mid-American Conference","Mountain West Conference","Pacific-10 Conference", "Southeastern Conference", "Sun Belt Conference", "Western Athletic Conference", "Ind", "Ivy", "Southwestern"};
	
	private JPanel ui;
	private JTextArea output;
	private JScrollPane scroll;
	private JLabel retrieveAll;
	private JLabel whoHave;
	private JButton go;
	private JButton save;
	private JLabel saveToFile;
	private JTextField filename;
	private static HashMap<String, String[]> table_attributes;

	
	private JTableSelector tables1;
	private JAttrSelector attrs1;
	
	//Query 4
	private JButton getHomeFieldAdvantage;
	private JComboBox<String> conferenceChoice;
	static Connection conn;
	
	DBWindow(){
		
		//Login Details
		dbSetupExample my = new dbSetupExample();
		
		//DB Setup
		try {
			Class.forName("org.postgresql.Driver");
			conn = DriverManager.getConnection("jdbc:postgresql://csce-315-db.engr.tamu.edu/team7_905_cfb", my.user, my.pswd);
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println(e.getClass().getName()+": "+e.getMessage());
			System.exit(0);
		}
		
		//GUI initialization
		setSize(800, 533);
		setTitle("Database GUI");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		//Output TextArea
		output = new JTextArea();
		output.setLineWrap(true);
		output.setEditable(false);
		output.setVisible(true);
		scroll = new JScrollPane(output);
		scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		
		ui = new JPanel();
		
		//HashMap values
		table_attributes = new HashMap<String, String[]>();
		table_attributes.put("", new String[] {""});
		table_attributes.put("Player", new String[]{"Player Code", "Last Name"});
		table_attributes.put("in_year", new String[]{"Player Code", "Year", "Team Code", "Uniform Number", "Height", "Weight", "Class", "Position"});
		table_attributes.put("Team", new String[]{"Team Code", "Name"});
		table_attributes.put("Game", new String[]{"Game Code", "Date", "Visit Team Code", "Home Team Code"});
		table_attributes.put("Stadium", new String[]{"Name", "City"});
		
		//Initialize Go Button
		go = new JButton("Go");
		go.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					//Create SQL Statement
					//Vulnerable to SQL injection
					
					String query = new String("SELECT * FROM  ");
					String[] table_names = tables1.getSelectItems();
					for(int i=0;i<table_names.length;i++) {
						query += removeNumFromTable(table_names[i]) + " AS " + table_names[i];
						if(i != table_names.length-1)
							query += ",";
					}
					if(!attrs1.get(0).isEmpty()) {
						query += " WHERE ";
						for (int i = 0; i < attrs1.current_size; i++) {
							if(i != 0)
								query += " AND ";
							query+= attrs1.get(i).toSQL();

						}
					}
					PreparedStatement pstmt = conn.prepareStatement(query);
					if(pstmt.execute()) { //There were results
						

						Font f = new Font(Font.MONOSPACED,Font.PLAIN,12);
						output.setFont(f);
						output.setText(resultSetToString(pstmt.getResultSet()));
					}
				} catch (SQLException f) {
					// TODO Auto-generated catch block
					f.printStackTrace();
				}
			}
		});
		
		//Initialize Save Button
		save = new JButton("Save");
		save.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					File file = new File(filename.getText());
					String toadd = "";
					file.createNewFile(); // creates new or overwrites old file with same name
					FileWriter writer = new FileWriter(file);
					toadd = output.getText();
					writer.write(toadd);
					writer.flush();
					writer.close();
				} catch (IOException f) {
					f.printStackTrace();
				}
			}
		});
		
		//Initialize attributes section
		attrs1 = new JAttrSelector(this, 6);
		
		//Initialize tables
		tables1 = new JTableSelector(this, TABLES, 6);
		tables1.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				attrs1.setAttributes(generateAllAttributes(tables1.getSelectItems()));
			}});
		
		//Initial values for attributes
		attrs1.setAttributes(generateAllAttributes(tables1.getSelectItems()));
		
		
		//Initialize getHomeFieldAdvantage Button
		getHomeFieldAdvantage = new JButton("Home Field Advantage");
		getHomeFieldAdvantage.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				//mean and std points for visitors in all conferences.
				double mean = 23.3621;
				double std = 13.6618;
				try {
					String query1 = new String("select avg(\"Visit Team Points\"), stddev_pop(\"Visit Team Points\") from game_results join Team on \"Home Team Code\"=\"Team Code\" join conference on team.\"Conference\"=conference.code where conference.\"name\"=? and \"Year\"='06/06/2012'");
					PreparedStatement pstmt = conn.prepareStatement(query1);
					pstmt.setString(1, (String) conferenceChoice.getSelectedItem());

					if(pstmt.execute()) {
						ResultSet rs = pstmt.getResultSet();
						rs.next();
						mean=rs.getDouble(1);
						std=rs.getDouble(2);
					}
					
	
					String query= new String("select team.\"Name\", avg(\"Home Team Points\"), stddev_pop(\"Home Team Points\")"
							+ " from game_results join Team on \"Home Team Code\"=\"Team Code\""
							+ " join conference on team.\"Conference\"=conference.code "
							+ "where conference.\"name\"=? and \"Year\"='06/06/2012' "
							+ "group by team.\"Name\" order by avg(\"Home Team Points\") desc;");
					pstmt = conn.prepareStatement(query);
					pstmt.setString(1, (String) conferenceChoice.getSelectedItem());
					
					if(pstmt.execute()) {
						ResultSet rs = pstmt.getResultSet();
						ResultSetMetaData md = rs.getMetaData();
						//Return String
						String return_string = new String();
						//Add Table Column names
						return_string += String.format("| %-20s", "Team");
						return_string += String.format("| %-20s", "Home Field Advantage");
						return_string += '|';
						return_string += "\n";
						return_string += "\n";
						//Add Table Data
						double hometeam_mean;
						double hometeam_std;
						while(rs.next()) {
							hometeam_mean = rs.getDouble(2);
							hometeam_std = rs.getDouble(3);
							
							return_string += String.format("| %-20s",rs.getString(1));
							return_string += String.format("| %-20s", 100*(1-cdf_approx( (mean-hometeam_mean) / Math.sqrt(std*std + hometeam_std*hometeam_std))));
							return_string += '|';
							return_string += "\n";
						}
						Font f = new Font(Font.MONOSPACED,Font.PLAIN,12);
						output.setFont(f);
						output.setText(return_string);
					}
				} catch (SQLException f) {
					// TODO Auto-generated catch block
					f.printStackTrace();
				}
				
			}
			
		});
		
		//Initialize
		conferenceChoice = new JComboBox<String>(CONFERENCES);
		update();
		
		setVisible(true);
	}
	
	public void update() {
		
		ui.setVisible(false);
		ui.removeAll();
		
		GridBagLayout group = new GridBagLayout();
		ui.setLayout(group);
		
		int ylvl = 0;
		
		//First Row
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = ylvl;
		c.gridwidth = 5;
		c.weightx = 0.5;
		c.ipady = 250;
		ui.add(scroll, c);
		
		ylvl++;
		//Second row
		retrieveAll = new JLabel("Retrieve all");
		c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = ylvl;
		c.weightx = 0.03;
		ui.add(retrieveAll, c);
		
		whoHave = new JLabel("who have");
		c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 2;
		c.gridy = ylvl;
		c.weightx = 0.03;
		c.insets = new Insets(0, PADDING, 0, 0);
		ui.add(whoHave, c);

		//Add Go button
		c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 4;
		c.gridy = 1;
		c.weightx = 0;
		c.insets = new Insets(0, PADDING, 0, 0);
		ui.add(go, c);
		
		//Add Tables
		c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 1;
		c.gridy = ylvl;
		c.gridheight = tables1.current_size+1;
		c.gridwidth = 1;
		c.weightx = 0.5;
		ui.add(tables1, c);
		
		//Add Attributes
		c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 3;
		c.gridy = ylvl;
		c.gridheight = attrs1.current_size+1;
		c.gridwidth = 1;
		c.weightx = 0.5;
		ui.add(attrs1, c);

		int maxSize = Math.max(attrs1.current_size, tables1.current_size);
		ylvl+=maxSize;		
		ylvl++;
		
		//Add save button
		c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 2;
		c.gridy = ylvl;
		c.weightx = 0.1;
		ui.add(save, c);

		//Add filename textfield
		filename = new JTextField();
		c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridwidth = 2;
		c.gridy = ylvl;
		c.weightx = 0.1;
		ui.add(filename, c);
		
		//Query 1
		ylvl++;
		
		//Query 2
		ylvl++;
		
		//Query 3
		ylvl++;

		//Query 4
		ylvl++;
		
		c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = ylvl;
		c.weightx = 0.1;
		ui.add(conferenceChoice, c);
		c.gridx = 1;
		c.gridwidth = 3;
		ui.add(getHomeFieldAdvantage, c);
		


		add(ui);

		ui.setVisible(true);
	}
	
	//Returns String[] of all attributes for all the select tables and "Custom Text"
	private String[] generateAllAttributes(String[] selectedTables) {
		
		ArrayList<String[]> output = new ArrayList<String[]>();
		for(String table: selectedTables)
			output.add(generateAttributes(table));
		int size = 0;
		for(int i=0;i<output.size();i++)
			size += output.get(i).length;
		
		//Increase size for Year and Custom Value
		size += 1;
		String[] real_output = new String[size];
		int curr = 0;
		for(int i=0;i<output.size();i++) {
			for(int j=0;j<output.get(i).length;j++) {
				real_output[curr] = output.get(i)[j];
				curr++;
			}
		}
		real_output[size-1] = "Custom Value";
		return real_output;
		
	}

	//Gets "Player" for "Player1"
	private String removeNumFromTable(String name) {
		if(Character.isDigit(name.charAt(name.length()-1))) {
			return name.substring(0,name.length()-1);
		}else {
			return name;
		}
	}
	//Returns a string containing the attribute names for the table
	//generateAttributes(Player1) would return
	//Player1.Player_Code etc.
	//Only works for tables with # < 10
	private String[] generateAttributes(String table) {
	
		String tableKey;
		if(Character.isDigit(table.charAt(table.length()-1))) {
			tableKey = table.substring(0,table.length()-1);
		}else {
			tableKey = table;
		}
		
		String[] attributeNames = table_attributes.get(tableKey);
		String[] output = new String[attributeNames.length];
		for(int i=0;i<output.length;i++)
			output[i] = table + "." + "\"" + attributeNames[i] +"\"";
		
		return output;
	}


String resultSetToString(ResultSet rs) throws SQLException {

	ResultSetMetaData md = rs.getMetaData();
	//Return String
	String return_string = new String();

	//Add Table Column names
	for(int i=1;i<md.getColumnCount()+1;i++) {
		return_string += String.format("| %-20s",md.getColumnLabel(i));
	}
	return_string += '|';
	return_string += "\n";
	//return_string += String.format("-".repeat(23*md.getColumnCount()+1));
	return_string += "\n";
	//Add Table Data
	while(rs.next()) {
		for(int i=1;i<md.getColumnCount()+1;i++) {
			return_string += String.format("| %-20s",rs.getString(i));
		}
		return_string += '|';
		return_string += "\n";
	}
	return return_string;
}

double cdf_approx(double z) {

	if(z>0) {
		return 1 - 0.5 * Math.exp(-(z*z+z)/2);
	}else{
		z = -z;
		return 0.5 * Math.exp(-(z*z+z)/2);
	}
}

}
