package com.neurotec.samples.swing;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableModel;

public final class NPropertyGrid extends JPanel implements ListSelectionListener {

	// ==============================================
	// Private static fields
	// ==============================================

	private static final long serialVersionUID = 1L;

	private static final Map<Class<?>, Class<?>> BOXED_TO_PRIMITIVE;

	// ==============================================
	// Static constructor
	// ==============================================

	static {
		BOXED_TO_PRIMITIVE = new HashMap<Class<?>, Class<?>>();
		BOXED_TO_PRIMITIVE.put(Integer.class, int.class);
		BOXED_TO_PRIMITIVE.put(Short.class, short.class);
		BOXED_TO_PRIMITIVE.put(Byte.class, byte.class);
		BOXED_TO_PRIMITIVE.put(Long.class, long.class);
		BOXED_TO_PRIMITIVE.put(Float.class, float.class);
		BOXED_TO_PRIMITIVE.put(Double.class, double.class);
		BOXED_TO_PRIMITIVE.put(Boolean.class, boolean.class);
	}

	// ==============================================
	// Private fields
	// ==============================================

	private final boolean isEditable;
	private List<String> supportedProperties;

	// ==============================================
	// Private GUI controls
	// ==============================================

	private final JTable table;
	private final JScrollPane scroll;
	private final JLabel propertyLabel;
	private final JTextArea txtDescription;
	private final JSplitPane gridSplitPane;

	// ==============================================
	// Public constructor
	// ==============================================

	public NPropertyGrid(boolean isEditable, List<String> supportedProperties) {
		super();
		setLayout(new BorderLayout());
		this.isEditable = isEditable;
		this.supportedProperties = supportedProperties;
		this.setMinimumSize(new Dimension(0, 0));
		table = new PropertyTable();
		table.setCellSelectionEnabled(true);
		table.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		scroll = new JScrollPane(table, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		gridSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

		JPanel descriptionPanel = new JPanel(new BorderLayout());
		propertyLabel = new JLabel();
		txtDescription = new JTextArea();
		txtDescription.setOpaque(false);
		txtDescription.setEditable(false);
		txtDescription.setLineWrap(true);
		txtDescription.setWrapStyleWord(true);
		txtDescription.setHighlighter(null);
		txtDescription.setBorder(BorderFactory.createEmptyBorder());
		descriptionPanel.add(propertyLabel, BorderLayout.BEFORE_FIRST_LINE);
		descriptionPanel.add(txtDescription, BorderLayout.CENTER);

		gridSplitPane.setLeftComponent(scroll);
		gridSplitPane.setRightComponent(descriptionPanel);
		gridSplitPane.setDividerLocation(250);
		gridSplitPane.setDividerSize(1);
		this.add(gridSplitPane);

		table.getSelectionModel().addListSelectionListener(this);
	}

	// ==============================================
	// Public methods
	// ==============================================

	public void setSource(Object bean) {
		if (bean == null) {
			return;
		}

		BeanTableModel model = new BeanTableModel(bean);
		table.setModel(model);
		table.getColumnModel().getColumn(1).setCellRenderer(new DefaultTableCellRenderer() {

			private static final long serialVersionUID = 1L;

			@Override
			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
				Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
				PropertyDescriptor prop = ((BeanTableModel) table.getModel()).getDescriptor(row);
				if (prop.getWriteMethod() != null) {
					Font currentFont = c.getFont();
					c.setFont(new Font(currentFont.getName(), Font.BOLD, currentFont.getSize()));
				}
				return c;
			}

		});

	}

	public void setSupportedProperties(List<String> supportedProperties) {
		this.supportedProperties = supportedProperties;
	}

	public void stopEditing() {
		if (table.getCellEditor() != null) {
			table.getCellEditor().stopCellEditing();
		}
	}

	// ==============================================
	// Event handling ListSelectionListener
	// ==============================================

	@Override
	public void valueChanged(ListSelectionEvent e) {
		int row = table.getSelectedRow();
		if (row > -1) {
			propertyLabel.setText((String) table.getValueAt(row, 0));
			Object value = table.getValueAt(row, 1);
			if (value == null) {
				txtDescription.setText("");
			} else {
				txtDescription.setText(value.toString());
			}
		}
	}

	// ==============================================
	// Private class implementing TableModel
	// ==============================================

	private class BeanTableModel implements TableModel {

		// ==============================================
		// Private fields
		// ==============================================

		private BeanInfo info;
		private Object object;
		private List<PropertyDescriptor> properties;
		private boolean showExpert;

		// ==============================================
		// Public constructor
		// ==============================================

		BeanTableModel(Object bean) {
			try {
				object = bean;
				info = Introspector.getBeanInfo(bean.getClass());
				properties = Collections.synchronizedList(new ArrayList<PropertyDescriptor>());
				showExpert = false;
				refreshProperties();
			} catch (IntrospectionException e) {
				e.printStackTrace();
				JOptionPane.showMessageDialog(NPropertyGrid.this, e.toString());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		// ==============================================
		// Private methods
		// ==============================================

		private void refreshProperties() {
			for (PropertyDescriptor prop : info.getPropertyDescriptors()) {
				if (supportedProperties.contains(prop.getName()) && ((!prop.isExpert()) || showExpert)) {
					properties.add(prop);
				}
			}
		}

		private Object getValueObject(PropertyDescriptor prop, Object obj, String newValue) {
			try {
				Object returnValue;
				Object readValue = prop.getReadMethod().invoke(obj);
				if (readValue instanceof Integer) {
					returnValue = Integer.valueOf(newValue);
				} else if (readValue instanceof Double) {
					returnValue = Double.valueOf(newValue);
				} else if (readValue instanceof Long) {
					returnValue = Long.valueOf(newValue);
				} else if (readValue instanceof Short) {
					returnValue = Short.valueOf(newValue);
				} else if (readValue instanceof Byte) {
					returnValue = Byte.valueOf(newValue);
				} else if (readValue instanceof Float) {
					returnValue = Float.valueOf(newValue);
				} else if (readValue instanceof Boolean) {
					returnValue = Boolean.valueOf(newValue);
				} else {
					returnValue = newValue;
				}
				return returnValue;
			} catch (InvocationTargetException e) {
				e.printStackTrace();
				JOptionPane.showMessageDialog(NPropertyGrid.this, e.getCause().toString());
			} catch (Exception e) {
				e.printStackTrace();
				JOptionPane.showMessageDialog(NPropertyGrid.this, e.toString());
			}
			return null;
		}

		private PropertyDescriptor getDescriptor(int row) {
			return properties.get(row);
		}

		private boolean isAssignable(Class<?> to, Class<?> from) {
			if (to.isAssignableFrom(from)) {
				return true;
			}
			Class<?> primitiveFrom = BOXED_TO_PRIMITIVE.get(from);
			if (primitiveFrom == null) {
				return false;
			} else {
				return to.isAssignableFrom(primitiveFrom);
			}
		}

		// ==============================================
		// Interface methods
		// ==============================================

		@Override
		public int getRowCount() {
			if ((info == null) || (info.getPropertyDescriptors() == null)) {
				return 0;
			} else {
				return properties.size();
			}
		}

		@Override
		public int getColumnCount() {
			return 2;
		}

		@Override
		public String getColumnName(int columnIndex) {
			return (columnIndex == 0) ? "Property" : "Value";
		}

		@Override
		public Class<?> getColumnClass(int columnIndex) {
			if (columnIndex == 0) {
				return String.class;
			} else {
				return Object.class;
			}
		}

		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex) {
			if (NPropertyGrid.this.isEditable) {
				if (columnIndex == 0) {
					return false;
				} else {
					PropertyDescriptor desc = properties.get(rowIndex);
					return desc.getWriteMethod() != null;
				}
			} else {
				return false;
			}

		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			PropertyDescriptor prop = properties.get(rowIndex);
			if (columnIndex == 0) {
				return prop.getDisplayName();
			} else {
				try {
					return prop.getReadMethod().invoke(object);
				} catch (InvocationTargetException e) {
					e.printStackTrace();
					JOptionPane.showMessageDialog(NPropertyGrid.this, e.getCause().toString());
				} catch (Exception e) {
					e.printStackTrace();
					JOptionPane.showMessageDialog(NPropertyGrid.this, e.toString());
				}
			}
			return "";
		}

		@Override
		public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
			try {
				PropertyDescriptor prop = properties.get(rowIndex);
				if (aValue instanceof String) {
					Object value = getValueObject(prop, object, (String) aValue);
					if ((value != null) && isAssignable(prop.getPropertyType(), value.getClass())) {
						prop.getWriteMethod().invoke(object, value);
					}
				} else {
					prop.getWriteMethod().invoke(object, aValue);
				}
				prop.getReadMethod().invoke(object);
			} catch (InvocationTargetException e) {
				e.printStackTrace();
				JOptionPane.showMessageDialog(NPropertyGrid.this, e.getCause().toString());
			} catch (Exception e) {
				e.printStackTrace();
				JOptionPane.showMessageDialog(NPropertyGrid.this, e.toString());
			}
		}

		@Override
		public void addTableModelListener(TableModelListener l) {
		}

		@Override
		public void removeTableModelListener(TableModelListener l) {
		}
	}

	// ==============================================
	// Private class extending JTable
	// ==============================================

	private final class PropertyTable extends JTable {

		// ==============================================
		// Private static fields
		// ==============================================

		private static final long serialVersionUID = 1L;

		// ==============================================
		// Overridden methods
		// ==============================================

		@Override
		public TableCellEditor getCellEditor(int row, int column) {
			if (table.getModel() instanceof BeanTableModel) {
				PropertyDescriptor prop = ((BeanTableModel) table.getModel()).getDescriptor(row);
				Class<?> propertyType = prop.getPropertyType();
				if (propertyType != null) {
					if (propertyType.equals(boolean.class)) {
						JComboBox booleanCmb = new JComboBox();
						booleanCmb.addItem(Boolean.TRUE);
						booleanCmb.addItem(Boolean.FALSE);
						return new DefaultCellEditor(booleanCmb);
					} else {
						Object[] enumConstansts = propertyType.getEnumConstants();
						if (enumConstansts != null && enumConstansts.length > 0) {
							return new DefaultCellEditor(new JComboBox(enumConstansts));
						}
					}
				}
			}

			return super.getCellEditor(row, column);
		}

	}

}
