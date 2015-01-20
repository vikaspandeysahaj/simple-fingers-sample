package com.neurotec.samples.panels;

import com.neurotec.biometrics.NBiometricStatus;
import com.neurotec.biometrics.NFinger;
import com.neurotec.biometrics.NSubject;
import com.neurotec.biometrics.swing.NFingerView;
import com.neurotec.biometrics.swing.NFingerViewBase.ShownImage;
import com.neurotec.images.NImage;
import com.neurotec.images.NImages;
import com.neurotec.io.NFile;
import com.neurotec.samples.settings.FingersTools;
import com.neurotec.samples.swing.ImageThumbnailFileChooser;
import com.neurotec.samples.util.Utils;
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
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;

public final class EnrollFromImage extends BasePanel implements ActionListener {

	// ===========================================================
	// Private static fields
	// ===========================================================

	private static final long serialVersionUID = 1L;

	// ===========================================================
	// Private fields
	// ===========================================================

	private NSubject subject;

	private final TemplateCreationHandler templateCreationHandler = new TemplateCreationHandler();

	private NFingerView viewImage;
	private NFingerView viewFinger;
	private ImageThumbnailFileChooser fcOpen;
	private JFileChooser fcSaveTemplate;
	private ImageThumbnailFileChooser fcSave;
	private File oldTemplateFile;
	private File oldImageFile;

	private JPanel actionButtonsPanel;
	private JPanel actionPanel;
	private JButton btnDefault;
	private JButton btnExtract;
	private JButton btnOpenImage;
	private JButton btnSaveImage;
	private JButton btnSaveTemplate;
	private JCheckBox cbShowProcessed;
	private JLabel lblQuality;
	private JPanel leftPanel;
	private JPanel optionsPanel;
	private JPanel rightPanel;
	private JPanel savePanel;
	private JScrollPane scrollPaneFinger;
	private JScrollPane scrollPaneImage;
	private JPanel southPanel;
	private JSpinner spinnerThreshold;
	private JSplitPane splitPane;
	private JLabel tresholdLabel;

	// ===========================================================
	// Public constructor
	// ===========================================================

	public EnrollFromImage() {
		super();
		requiredLicenses = new ArrayList<String>();
		requiredLicenses.add("Biometrics.FingerExtraction");
		optionalLicenses = new ArrayList<String>();
		optionalLicenses.add("Images.WSQ");
	}

	// ===========================================================
	// Private methods
	// ===========================================================

	private void openImage() throws IOException {
		if (fcOpen.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
			NFinger finger = new NFinger();
			finger.setImage(NImage.fromFile(fcOpen.getSelectedFile().getAbsolutePath()));
			viewImage.setFinger(finger);
			viewFinger.setFinger(null);
			subject = null;
			lblQuality.setText("");
			updateControls();
			createTemplate();
		}
	}

	private void createTemplate() {
		subject = new NSubject();
		NFinger finger = new NFinger();
		finger.setImage(viewImage.getFinger().getImage());
		subject.getFingers().add(finger);
		updateFingersTools();
		FingersTools.getInstance().getClient().createTemplate(subject, null, templateCreationHandler);
	}

	private void saveTemplate() throws IOException {
		if (subject == null) {
			return;
		}
		if (oldTemplateFile != null) {
			fcSaveTemplate.setSelectedFile(oldTemplateFile);
		}
		if (fcSaveTemplate.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
			oldTemplateFile = fcSaveTemplate.getSelectedFile();
			String fileName = fcSaveTemplate.getSelectedFile().getAbsolutePath();
			NFile.writeAllBytes(fileName, subject.getTemplateBuffer());
		}
	}

	private void saveImage() throws IOException {
		if (subject == null) {
			return;
		}
		if (oldImageFile != null) {
			fcSave.setSelectedFile(oldImageFile);
		}
		if (fcSave.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
			oldImageFile = fcSave.getSelectedFile();
			String fileName = fcSave.getSelectedFile().getAbsolutePath();
			if (cbShowProcessed.isSelected()) {
				subject.getFingers().get(0).getProcessedImage().save(fileName);
			} else {
				subject.getFingers().get(0).getImage().save(fileName);
			}
		}
	}

	private void updateTemplateCreationStatus(boolean created) {
		if (created) {
			viewFinger.setFinger(subject.getFingers().get(0));
			lblQuality.setText(String.format("Quality: %d", (subject.getFingers().get(0).getObjects().get(0).getQuality() & 0xFF)));
		} else {
			viewFinger.setFinger(null);
			lblQuality.setText("");
		}
		updateControls();
	}

	// ===========================================================
	// Protected methods
	// ===========================================================

	@Override
	protected void initGUI() {
		GridBagConstraints gridBagConstraints;

		splitPane = new JSplitPane();
		leftPanel = new JPanel();
		scrollPaneImage = new JScrollPane();
		rightPanel = new JPanel();
		scrollPaneFinger = new JScrollPane();
		southPanel = new JPanel();
		actionPanel = new JPanel();
		actionButtonsPanel = new JPanel();
		btnOpenImage = new JButton();
		btnExtract = new JButton();
		savePanel = new JPanel();
		btnSaveImage = new JButton();
		btnSaveTemplate = new JButton();
		lblQuality = new JLabel();
		optionsPanel = new JPanel();
		btnDefault = new JButton();
		tresholdLabel = new JLabel();
		spinnerThreshold = new JSpinner();
		cbShowProcessed = new JCheckBox();

		setLayout(new BorderLayout());

		leftPanel.setLayout(new GridLayout(1, 1));

		scrollPaneImage.setMinimumSize(new Dimension(100, 100));
		scrollPaneImage.setPreferredSize(new Dimension(200, 200));
		leftPanel.add(scrollPaneImage);

		splitPane.setLeftComponent(leftPanel);

		rightPanel.setLayout(new GridLayout(1, 1));

		scrollPaneFinger.setMinimumSize(new Dimension(100, 100));
		scrollPaneFinger.setPreferredSize(new Dimension(200, 200));
		rightPanel.add(scrollPaneFinger);

		splitPane.setRightComponent(rightPanel);

		splitPane.setResizeWeight(0.5);

		add(splitPane, BorderLayout.CENTER);

		southPanel.setLayout(new BorderLayout());

		actionPanel.setLayout(new BorderLayout());

		btnOpenImage.setText("Open image");
		actionButtonsPanel.add(btnOpenImage);

		btnExtract.setText("Extract features");
		btnExtract.setEnabled(false);
		actionButtonsPanel.add(btnExtract);

		actionPanel.add(actionButtonsPanel, BorderLayout.WEST);

		southPanel.add(actionPanel, BorderLayout.WEST);

		btnSaveImage.setText("Save image");
		btnSaveImage.setEnabled(false);
		savePanel.add(btnSaveImage);

		btnSaveTemplate.setText("Save template");
		btnSaveTemplate.setEnabled(false);
		savePanel.add(btnSaveTemplate);
		savePanel.add(lblQuality);

		southPanel.add(savePanel, BorderLayout.SOUTH);

		optionsPanel.setLayout(new GridBagLayout());

		btnDefault.setText("Default");
		gridBagConstraints = new GridBagConstraints();
		gridBagConstraints.gridx = 2;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.insets = new Insets(3, 3, 3, 3);
		optionsPanel.add(btnDefault, gridBagConstraints);

		tresholdLabel.setText("Treshold");
		gridBagConstraints = new GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.anchor = GridBagConstraints.LINE_END;
		gridBagConstraints.insets = new Insets(3, 3, 3, 3);
		optionsPanel.add(tresholdLabel, gridBagConstraints);

		spinnerThreshold.setModel(new SpinnerNumberModel(Byte.valueOf((byte) 0), Byte.valueOf((byte) 0), Byte.valueOf((byte) 100), Byte.valueOf((byte) 1)));
		spinnerThreshold.setPreferredSize(new Dimension(50, 20));
		gridBagConstraints = new GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.insets = new Insets(3, 3, 3, 3);
		optionsPanel.add(spinnerThreshold, gridBagConstraints);

		cbShowProcessed.setText("Show processed image");
		gridBagConstraints = new GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 1;
		gridBagConstraints.gridwidth = 2;
		gridBagConstraints.anchor = GridBagConstraints.LINE_END;
		optionsPanel.add(cbShowProcessed, gridBagConstraints);

		southPanel.add(optionsPanel, BorderLayout.EAST);

		add(southPanel, BorderLayout.SOUTH);

		panelLicensing = new LicensingPanel(requiredLicenses, optionalLicenses);
		add(panelLicensing, java.awt.BorderLayout.NORTH);

		fcOpen = new ImageThumbnailFileChooser();
		fcOpen.setIcon(Utils.createIconImage("images/Logo16x16.png"));
		fcOpen.setFileFilter(new Utils.ImageFileFilter(NImages.getOpenFileFilter()));
		fcSaveTemplate = new JFileChooser();
		fcSave = new ImageThumbnailFileChooser();
		fcSave.setIcon(Utils.createIconImage("images/Logo16x16.png"));
		fcSave.setFileFilter(new Utils.ImageFileFilter(NImages.getSaveFileFilter()));
		viewImage = new NFingerView();
		scrollPaneImage.setViewportView(viewImage);
		viewFinger = new NFingerView();
		viewFinger.setShownImage(ShownImage.RESULT);
		scrollPaneFinger.setViewportView(viewFinger);
		viewFinger.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent ev) {
				super.mouseClicked(ev);
				if (ev.getButton() == MouseEvent.BUTTON3) {
					cbShowProcessed.doClick();
				}
			}
		});
		cbShowProcessed.setSelected(true);

		btnDefault.addActionListener(this);
		btnExtract.addActionListener(this);
		btnOpenImage.addActionListener(this);
		btnSaveImage.addActionListener(this);
		btnSaveTemplate.addActionListener(this);
		cbShowProcessed.addActionListener(this);
	}

	@Override
	protected void setDefaultValues() {
		spinnerThreshold.setValue(FingersTools.getInstance().getDefaultClient().getFingersQualityThreshold());
	}

	@Override
	protected void updateControls() {
		btnExtract.setEnabled((viewImage.getFinger() != null) && (viewImage.getFinger().getImage() != null));
		btnSaveImage.setEnabled((subject != null) && (subject.getStatus() == NBiometricStatus.OK));
		btnSaveTemplate.setEnabled((subject != null) && (subject.getStatus() == NBiometricStatus.OK));
		cbShowProcessed.setEnabled((subject != null) && (subject.getStatus() == NBiometricStatus.OK));
	}

	@Override
	protected void updateFingersTools() {
		FingersTools.getInstance().getClient().reset();
		FingersTools.getInstance().getClient().setFingersReturnProcessedImage(true);
		FingersTools.getInstance().getClient().setFingersQualityThreshold((Byte) spinnerThreshold.getValue());
	}

	// ===========================================================
	// Event handling
	// ===========================================================

	@Override
	public void actionPerformed(ActionEvent ev) {
		try {
			if (ev.getSource() == btnDefault) {
				spinnerThreshold.setValue(FingersTools.getInstance().getDefaultClient().getFingersQualityThreshold());
			} else if (ev.getSource() == btnOpenImage) {
				openImage();
			} else if (ev.getSource() == btnExtract) {
				createTemplate();
			} else if (ev.getSource() == btnSaveTemplate) {
				saveTemplate();
			} else if (ev.getSource() == btnSaveImage) {
				saveImage();
			} else if (ev.getSource() == cbShowProcessed) {
				if (cbShowProcessed.isSelected()) {
					viewFinger.setShownImage(ShownImage.RESULT);
				} else {
					viewFinger.setShownImage(ShownImage.ORIGINAL);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(this, e, "Error", JOptionPane.ERROR_MESSAGE);
			updateControls();
		}
	}

	// ===========================================================
	// Inner classes
	// ===========================================================

	private class TemplateCreationHandler implements CompletionHandler<NBiometricStatus, Object> {

		@Override
		public void completed(final NBiometricStatus result, final Object attachment) {
			SwingUtilities.invokeLater(new Runnable() {

				@Override
				public void run() {
					if (result == NBiometricStatus.OK) {
						updateTemplateCreationStatus(true);
					} else if (result == NBiometricStatus.BAD_OBJECT) {
						JOptionPane.showMessageDialog(EnrollFromImage.this, "Finger image quality is too low.");
						updateTemplateCreationStatus(false);
					} else {
						JOptionPane.showMessageDialog(EnrollFromImage.this, result);
						updateTemplateCreationStatus(false);
					}
				}
			});
		}

		@Override
		public void failed(final Throwable th, final Object attachment) {
			SwingUtilities.invokeLater(new Runnable() {

				@Override
				public void run() {
					th.printStackTrace();
					showErrorDialog(th);
					updateTemplateCreationStatus(false);
				}

			});
		}

	}

}
