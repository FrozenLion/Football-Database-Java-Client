import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.*;

public class ConditionalOption extends JComponent {
	
	public static final int PADDING = 1;
	
	private JComboBox<String> attributes;
	private JComboBox<String> condition;
	private JTextField input;
	
	public ConditionalOption() {
		
		GridBagLayout group = new GridBagLayout();
		setLayout(group);

		String[] attrList = new String[] {"a", "b"};
		attributes = new JComboBox<String>(attrList);
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 0.5;
		c.weighty = 0.5;
		add(attributes, c);

		c = new GridBagConstraints();
		String[] condList = new String[] {"a", "b"};
		condition = new JComboBox<String>(condList);
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 1;
		c.gridy = 0;
		c.weightx = 0.5;
		c.weighty = 0.5;
		c.insets = new Insets(0, 2, 0, 0);
		add(condition, c);

		c = new GridBagConstraints();
		input = new JTextField();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 2;
		c.gridy = 0;
		c.weightx = 0.5;
		c.weighty = 0.5;
		c.insets = new Insets(0, 2, 0, 0);
	}
}
