package com.neurotec.samples;

import com.neurotec.biometrics.NBiometricStatus;
import com.neurotec.biometrics.NFinger;
import com.neurotec.biometrics.NSubject;
import com.neurotec.biometrics.NSubject.FingerCollection;
import com.neurotec.biometrics.swing.NFingerView;
import com.neurotec.biometrics.swing.NFingerViewBase.ShownImage;
import com.neurotec.samples.swing.ImageThumbnailFileChooser;
import com.neurotec.samples.util.Utils;
import com.neurotec.util.NIndexPair;
import com.neurotec.util.concurrent.CompletionHandler;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.Box.Filler;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;

public final class VerifyFinger extends BasePanel implements ActionListener {

	// ===========================================================
	// Private static fields
	// ===========================================================

	private static final long serialVersionUID = 1L;

	private static final String SUBJECT_LEFT = "left";
	private static final String SUBJECT_RIGHT = "right";

	private static final String LEFT_LABEL_TEXT = "Image or template left: ";
	private static final String RIGHT_LABEL_TEXT = "Image or template right: ";

	// ===========================================================
	// Private fields
	// ===========================================================

	private NSubject subjectLeft;
	private NSubject subjectRight;
	private NFingerView viewLeft;
	private NFingerView viewRight;
	private ImageThumbnailFileChooser fileChooser;

	private final TemplateCreationHandler templateCreationHandler = new TemplateCreationHandler();
	private final VerificationHandler verificationHandler = new VerificationHandler();

	private JCheckBox cbShowProcessed;
	private JPanel centerPanel;
	private JButton clearButton;
	private JPanel clearButtonPanel;
	private JButton defaultButton;
	private JComboBox farComboBox;
	private JPanel farPanel;
	private Filler filler1;
	private Filler filler2;
	private Filler filler3;
	private JPanel imageControlsPanel;
	private JLabel leftLabel;
	private JButton leftOpenButton;
	private JScrollPane leftScrollPane;
	private JPanel mainPanel;
	private JPanel northPanel;
	private JLabel rightLabel;
	private JButton rightOpenButton;
	private JScrollPane rightScrollPane;
	private JPanel showProcessedPanel;
	private JPanel southPanel;
	private JButton verifyButton;
	private JLabel verifyLabel;
	private JPanel verifyPanel;

	// ===========================================================
	// Public constructor
	// ===========================================================

	public VerifyFinger() {
		super();
		requiredLicenses = new ArrayList<String>();
		requiredLicenses.add("Biometrics.FingerExtraction");
		requiredLicenses.add("Biometrics.FingerMatching");
		optionalLicenses = new ArrayList<String>();
		optionalLicenses.add("Images.WSQ");

		subjectLeft = new NSubject();
		subjectRight = new NSubject();
	}

	// ===========================================================
	// Private methods
	// ===========================================================

	private void loadItem(String position) throws IOException {
		fileChooser.setMultiSelectionEnabled(false);
		if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
			verifyLabel.setText("");
			NSubject subjectTmp = null;
			NFinger finger = null;
			try {
				subjectTmp = NSubject.fromFile(fileChooser.getSelectedFile().getAbsolutePath());
				FingerCollection fingers = subjectTmp.getFingers();
				if (fingers.isEmpty()) {
					subjectTmp = null;
					throw new IllegalArgumentException("Template contains no finger records.");
				}
				finger = fingers.get(0);
				templateCreationHandler.completed(NBiometricStatus.OK, position);
			} catch (UnsupportedOperationException e) {
				// Ignore. UnsupportedOperationException means file is not a valid template.
			}

			// If file is not a template, try to load it as an image.
			if (subjectTmp == null) {
				finger = new NFinger();
				finger.setFileName(fileChooser.getSelectedFile().getAbsolutePath());
				subjectTmp = new NSubject();
				subjectTmp.getFingers().add(finger);
				updateFingersTools();
				FingersTools.getInstance().getClient().createTemplate(subjectTmp, position, templateCreationHandler);
			}

			if (SUBJECT_LEFT.equals(position)) {
				subjectLeft = subjectTmp;
				leftLabel.setText(fileChooser.getSelectedFile().getAbsolutePath());
				viewLeft.setFinger(finger);
			} else if (SUBJECT_RIGHT.equals(position)) {
				subjectRight = subjectTmp;
				rightLabel.setText(fileChooser.getSelectedFile().getAbsolutePath());
				viewRight.setFinger(finger);
			} else {
				throw new AssertionError("Unknown subject position: " + position);
			}
		}
	}

	private void verify() {
		updateFingersTools();
		FingersTools.getInstance().getClient().verify(subjectLeft, subjectRight, null, verificationHandler);
	}

	private void clear() {
		viewLeft.setFinger(null);
		viewRight.setFinger(null);
		subjectLeft.clear();
		subjectRight.clear();
		updateControls();
		verifyLabel.setText(" ");
		leftLabel.setText(LEFT_LABEL_TEXT);
		rightLabel.setText(RIGHT_LABEL_TEXT);
	}

	// ===========================================================
	// Protected methods
	// ===========================================================

	@Override
	protected void initGUI() {
		GridBagConstraints gridBagConstraints;

		mainPanel = new JPanel();
		northPanel = new JPanel();
		leftOpenButton = new JButton();
		farPanel = new JPanel();
		farComboBox = new JComboBox();
		defaultButton = new JButton();
		rightOpenButton = new JButton();
		centerPanel = new JPanel();
		leftScrollPane = new JScrollPane();
		rightScrollPane = new JScrollPane();
		southPanel = new JPanel();
		imageControlsPanel = new JPanel();
		showProcessedPanel = new JPanel();
		cbShowProcessed = new JCheckBox();
		clearButtonPanel = new JPanel();
		clearButton = new JButton();
		verifyPanel = new JPanel();
		leftLabel = new JLabel();
		filler1 = new Filler(new Dimension(0, 3), new Dimension(0, 3), new Dimension(32767, 3));
		rightLabel = new JLabel();
		filler2 = new Filler(new Dimension(0, 3), new Dimension(0, 3), new Dimension(32767, 3));
		verifyButton = new JButton();
		filler3 = new Filler(new Dimension(0, 3), new Dimension(0, 3), new Dimension(32767, 3));
		verifyLabel = new JLabel();

		setLayout(new BorderLayout());

		mainPanel.setLayout(new BorderLayout());

		leftOpenButton.setText("Open");
		northPanel.add(leftOpenButton);

		farPanel.setBorder(BorderFactory.createTitledBorder(null, "Matching FAR", TitledBorder.CENTER, TitledBorder.DEFAULT_POSITION));
		farPanel.setLayout(new GridBagLayout());

		char c = new DecimalFormatSymbols().getPercent();
		DefaultComboBoxModel model = (DefaultComboBoxModel) farComboBox.getModel();
		NumberFormat nf = NumberFormat.getNumberInstance();
		nf.setMaximumFractionDigits(5);
		model.addElement(nf.format(0.1) + c);
		model.addElement(nf.format(0.01) + c);
		model.addElement(nf.format(0.001) + c);
		farComboBox.setSelectedIndex(1);
		farComboBox.setEditable(true);
		farComboBox.setModel(model);
		gridBagConstraints = new GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.insets = new Insets(3, 3, 3, 3);
		farPanel.add(farComboBox, gridBagConstraints);

		defaultButton.setText("Default");
		gridBagConstraints = new GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.insets = new Insets(3, 3, 3, 3);
		farPanel.add(defaultButton, gridBagConstraints);

		northPanel.add(farPanel);

		rightOpenButton.setText("Open");
		northPanel.add(rightOpenButton);

		mainPanel.add(northPanel, BorderLayout.NORTH);

		centerPanel.setLayout(new GridLayout(1, 2, 5, 0));

		leftScrollPane.setPreferredSize(new Dimension(200, 200));
		centerPanel.add(leftScrollPane);

		rightScrollPane.setPreferredSize(new Dimension(200, 200));
		centerPanel.add(rightScrollPane);

		mainPanel.add(centerPanel, BorderLayout.CENTER);

		southPanel.setLayout(new BorderLayout());

		imageControlsPanel.setLayout(new BoxLayout(imageControlsPanel, BoxLayout.Y_AXIS));

		cbShowProcessed.setText("Show processed image");
		showProcessedPanel.add(cbShowProcessed);

		imageControlsPanel.add(showProcessedPanel);

		clearButton.setText("Clear images");
		clearButtonPanel.add(clearButton);

		imageControlsPanel.add(clearButtonPanel);

		southPanel.add(imageControlsPanel, BorderLayout.NORTH);

		verifyPanel.setLayout(new BoxLayout(verifyPanel, BoxLayout.Y_AXIS));

		leftLabel.setText(LEFT_LABEL_TEXT);
		verifyPanel.add(leftLabel);
		verifyPanel.add(filler1);

		rightLabel.setText(RIGHT_LABEL_TEXT);
		verifyPanel.add(rightLabel);
		verifyPanel.add(filler2);

		verifyButton.setText("Verify");
		verifyButton.setEnabled(false);
		verifyPanel.add(verifyButton);
		verifyPanel.add(filler3);

		verifyLabel.setText("     ");
		verifyPanel.add(verifyLabel);

		southPanel.add(verifyPanel, BorderLayout.WEST);

		mainPanel.add(southPanel, BorderLayout.SOUTH);

		add(mainPanel, BorderLayout.CENTER);

		panelLicensing = new LicensingPanel(requiredLicenses, optionalLicenses);
		add(panelLicensing, java.awt.BorderLayout.NORTH);

		fileChooser = new ImageThumbnailFileChooser();
		fileChooser.setIcon(Utils.createIconImage("images/Logo16x16.png"));
		viewRight = new NFingerView();
		rightScrollPane.setViewportView(viewRight);
		viewLeft = new NFingerView();
		leftScrollPane.setViewportView(viewLeft);
		viewRight.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent ev) {
				super.mouseClicked(ev);
				if (ev.getButton() == MouseEvent.BUTTON3) {
					cbShowProcessed.doClick();
				}
			}
		});
		viewLeft.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent ev) {
				super.mouseClicked(ev);
				if (ev.getButton() == MouseEvent.BUTTON3) {
					cbShowProcessed.doClick();
				}
			}
		});

		leftOpenButton.addActionListener(this);
		rightOpenButton.addActionListener(this);
		defaultButton.addActionListener(this);
		clearButton.addActionListener(this);
		verifyButton.addActionListener(this);
		cbShowProcessed.addActionListener(this);

		cbShowProcessed.doClick();
	}

	@Override
	protected void setDefaultValues() {
		farComboBox.setSelectedItem(Utils.matchingThresholdToString(FingersTools.getInstance().getDefaultClient().getMatchingThreshold()));
	}

	@Override
	protected void updateControls() {
		if (subjectLeft.getFingers().isEmpty()
			|| (subjectLeft.getFingers().get(0).getObjects().get(0).getTemplate() == null)
			|| subjectRight.getFingers().isEmpty()
			|| (subjectRight.getFingers().get(0).getObjects().get(0).getTemplate() == null)) {
			verifyButton.setEnabled(false);
		} else {
			verifyButton.setEnabled(true);
		}
	}

	@Override
	protected void updateFingersTools() {
		FingersTools.getInstance().getClient().reset();
		FingersTools.getInstance().getClient().setFingersReturnProcessedImage(true);
		FingersTools.getInstance().getClient().setMatchingWithDetails(true);
		try {
			FingersTools.getInstance().getClient().setMatchingThreshold(Utils.matchingThresholdFromString(farComboBox.getSelectedItem().toString()));
		} catch (ParseException e) {
			e.printStackTrace();
			FingersTools.getInstance().getClient().setMatchingThreshold(FingersTools.getInstance().getDefaultClient().getMatchingThreshold());
			farComboBox.setSelectedItem(Utils.matchingThresholdToString(FingersTools.getInstance().getDefaultClient().getMatchingThreshold()));
			JOptionPane.showMessageDialog(this, "FAR is not valid. Using default value.", "Error", JOptionPane.ERROR_MESSAGE);
		}
	}

	// ===========================================================
	// Package private methods
	// ===========================================================

	void updateLabel(String msg) {
		verifyLabel.setText(msg);
	}

	NSubject getLeft() {
		return subjectLeft;
	}

	NSubject getRight() {
		return subjectRight;
	}

	// ===========================================================
	// Event handling
	// ===========================================================

	@Override
	public void actionPerformed(ActionEvent ev) {
		try {
			if (ev.getSource() == defaultButton) {
				farComboBox.setSelectedItem(Utils.matchingThresholdToString(FingersTools.getInstance().getDefaultClient().getMatchingThreshold()));
			} else if (ev.getSource() == verifyButton) {
				verify();
			} else if (ev.getSource() == leftOpenButton) {
				loadItem(SUBJECT_LEFT);
			} else if (ev.getSource() == rightOpenButton) {
				loadItem(SUBJECT_RIGHT);
			} else if (ev.getSource() == clearButton) {
				clear();
			} else if (ev.getSource() == cbShowProcessed) {
				if (cbShowProcessed.isSelected()) {
					viewLeft.setShownImage(ShownImage.RESULT);
					viewRight.setShownImage(ShownImage.RESULT);
				} else {
					viewLeft.setShownImage(ShownImage.ORIGINAL);
					viewRight.setShownImage(ShownImage.ORIGINAL);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(this, e, "Error", JOptionPane.ERROR_MESSAGE);
		}
	}

	// ===========================================================
	// Inner classes
	// ===========================================================

	private class TemplateCreationHandler implements CompletionHandler<NBiometricStatus, String> {

		@Override
		public void completed(final NBiometricStatus status, final String subject) {
			SwingUtilities.invokeLater(new Runnable() {

				@Override
				public void run() {
					if (status != NBiometricStatus.OK) {
						JOptionPane.showMessageDialog(VerifyFinger.this, "Template was not created: " + status, "Error", JOptionPane.WARNING_MESSAGE);
					}
					updateControls();
				}

			});
		}

		@Override
		public void failed(final Throwable th, final String subject) {
			SwingUtilities.invokeLater(new Runnable() {

				@Override
				public void run() {
					th.printStackTrace();
					showErrorDialog(th);
				}

			});
		}

	}

	private class VerificationHandler implements CompletionHandler<NBiometricStatus, String> {

		@Override
		public void completed(final NBiometricStatus status, final String subject) {
			SwingUtilities.invokeLater(new Runnable() {

				@Override
				public void run() {
					if (status == NBiometricStatus.OK) {
						int score = getLeft().getMatchingResults().get(0).getScore();
						String msg = "Score of matched templates: " + score;
						updateLabel(msg);
						JOptionPane.showMessageDialog(VerifyFinger.this, msg, "Match", JOptionPane.PLAIN_MESSAGE);

						NIndexPair[] matedMinutiae = getLeft().getMatchingResults().get(0).getMatchingDetails().getFingers().get(0).getMatedMinutiae();

						viewLeft.setMatedMinutiaIndex(0);
						viewLeft.setMatedMinutiae(matedMinutiae);

						viewRight.setMatedMinutiaIndex(1);
						viewRight.setMatedMinutiae(matedMinutiae);

						viewLeft.prepareTree();
						viewRight.setTree(viewLeft.getTree());
					} else {
						JOptionPane.showMessageDialog(VerifyFinger.this, "Templates didn't match.", "No match", JOptionPane.WARNING_MESSAGE);
					}
				}

			});
		}

		@Override
		public void failed(final Throwable th, final String subject) {
			SwingUtilities.invokeLater(new Runnable() {

				@Override
				public void run() {
					th.printStackTrace();
					showErrorDialog(th);
				}

			});
		}

	}

}
