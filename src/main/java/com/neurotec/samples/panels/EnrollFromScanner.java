package com.neurotec.samples.panels;

import com.neurotec.biometrics.*;
import com.neurotec.biometrics.swing.NFingerView;
import com.neurotec.biometrics.swing.NFingerViewBase.ShownImage;
import com.neurotec.devices.NDevice;
import com.neurotec.devices.NDeviceManager;
import com.neurotec.devices.NDeviceType;
import com.neurotec.devices.NFingerScanner;
import com.neurotec.images.NImages;
import com.neurotec.io.NBuffer;
import com.neurotec.samples.model.PatientFingerPrintModel;
import com.neurotec.samples.services.FingerPrintService;
import com.neurotec.samples.settings.FingersTools;
import com.neurotec.samples.util.Utils;
import com.neurotec.util.concurrent.CompletionHandler;
import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;
import org.json.JSONException;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.SoftBevelBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.applet.Applet;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.*;

public final class EnrollFromScanner extends BasePanel implements ActionListener {

    private static final long serialVersionUID = 1L;

    private final NDeviceManager deviceManager;
    private final CaptureCompletionHandler captureCompletionHandler = new CaptureCompletionHandler();
    private final EnrollCompletionHandler enrollCompletionHandler = new EnrollCompletionHandler();
    private NSubject subject;
    private boolean scanning;
    private NFingerView view;
    private JFileChooser fcImage;
    private JFileChooser fcTemplate;
    private File oldImageFile;
    private File oldTemplateFile;

    private JButton btnCancel;
    private JButton btnForce;
    private JButton btnRefresh;
    private JButton btnIdentifyPatient;
    private JButton btnScan;
    private JCheckBox cbAutomatic;
    private JCheckBox cbShowProcessed;
    private JLabel lblInfo;
    private JPanel panelButtons;
    private JPanel panelInfo;
    private JPanel panelMain;
    private JPanel panelSave;
    private JPanel panelScanners;
    private JPanel panelSouth;
    private JList scannerList;
    private JScrollPane scrollPane;
    private JScrollPane scrollPaneList;

    private FingerPrintService service;
    java.util.List<PatientFingerPrintModel> patients;

    public EnrollFromScanner() {
        super();
        requiredLicenses = new ArrayList<String>();
        requiredLicenses.add("Biometrics.FingerExtraction");
        requiredLicenses.add("Biometrics.FingerMatchingFast");
        requiredLicenses.add("Biometrics.FingerMatching");
        requiredLicenses.add("Biometrics.FingerQualityAssessment");
        requiredLicenses.add("Biometrics.FingerSegmentation");
        requiredLicenses.add("Biometrics.FingerSegmentsDetection");

        requiredLicenses.add("Biometrics.Standards.Fingers");
        requiredLicenses.add("Biometrics.Standards.FingerTemplates");
        requiredLicenses.add("Devices.FingerScanners");


        optionalLicenses = new ArrayList<String>();
        optionalLicenses.add("Images.WSQ");

        FingersTools.getInstance().getClient().setUseDeviceManager(true);
        deviceManager = FingersTools.getInstance().getClient().getDeviceManager();
        deviceManager.setDeviceTypes(EnumSet.of(NDeviceType.FINGER_SCANNER));
        deviceManager.initialize();

    }

    // ===========================================================
    // Private methods
    // ===========================================================

    private void startCapturing() {
        lblInfo.setText("");
        if (FingersTools.getInstance().getClient().getFingerScanner() == null) {
            JOptionPane.showMessageDialog(this, "Please select scanner from the list.", "No scanner selected", JOptionPane.PLAIN_MESSAGE);
            return;
        }

        // Create a finger.
        NFinger finger = new NFinger();

        // Set Manual capturing mode if automatic isn't selected.
        if (!cbAutomatic.isSelected()) {
            finger.setCaptureOptions(EnumSet.of(NBiometricCaptureOption.MANUAL));
        }

        // Add finger to subject and finger view.
        subject = new NSubject();
        subject.getFingers().add(finger);
        view.setFinger(finger);
        view.setShownImage(ShownImage.ORIGINAL);

        // Begin capturing.
        NBiometricTask task = FingersTools.getInstance().getClient().createTask(EnumSet.of(NBiometricOperation.CAPTURE, NBiometricOperation.CREATE_TEMPLATE), subject);
        FingersTools.getInstance().getClient().performTask(task, null, captureCompletionHandler);
        scanning = true;
        updateControls();
    }

    private void identifyPatient() throws IOException, JSONException {

        service = new FingerPrintService((Applet)this.getParent().getParent().getParent().getParent().getParent().getParent().getParent());

        //1. make server call to get java.util.List<PatientFingerPrintModel> patientModels
        this.patients = service.getAllPatients();

        //2. enroll the patientModels
        this.enrollFingerPrints(patients);

        //3.identify the patient id
        String patientID = this.identifyFinger();

        //4. set the patient view
        PatientFingerPrintModel patient = getPatientByUUID(patientID);
        if(patient!= null){
            service.updatePatientListView(patient);
        }
        else {
            JOptionPane.showMessageDialog(this, "Patient Not Found", "Identification", JOptionPane.PLAIN_MESSAGE);
        }
    }

    private PatientFingerPrintModel getPatientByUUID(String patientID){
        for (PatientFingerPrintModel patient : patients){
            if(patient.getPatientUUID().equals(patientID)){
                return patient;
            }
        }
        return null;
    }
    public void enrollFingerPrints(java.util.List<PatientFingerPrintModel> patientModels) throws IOException {
        NBiometricTask enrollTask = FingersTools.getInstance().getClient().createTask(EnumSet.of(NBiometricOperation.ENROLL), null);
        if (patientModels.size() > 0) {
            for (PatientFingerPrintModel model : patientModels) {
                NTemplate template = createTemplate(model.getFingerprintTemplate());
                enrollTask.getSubjects().add(createSubject(template, model.getPatientUUID()));
            }
            FingersTools.getInstance().getClient().performTask(enrollTask, NBiometricOperation.ENROLL, enrollCompletionHandler);
        } else {
            return;
        }

    }

    public String identifyFinger() throws IOException {

        NBiometricTask task = FingersTools.getInstance().getClient().createTask(EnumSet.of(NBiometricOperation.IDENTIFY), subject);
        NBiometricStatus status = FingersTools.getInstance().getClient().identify(subject);
        if (status == NBiometricStatus.OK) {
            for (NMatchingResult result : subject.getMatchingResults()) {
                return result.getId();
            }
        }
        return "PATIENT_NOT_FOUND";

    }

    private NTemplate createTemplate(String fingerPrintTemplateString) {
        byte[] templateBuffer = Base64.decode(fingerPrintTemplateString);
        return new NTemplate(new NBuffer(templateBuffer));
    }

    private NSubject createSubject(NTemplate template, String id) throws IOException {
        NSubject subject = new NSubject();
        subject.setTemplate(template);
        subject.setId(id);
        return subject;
    }

    private void saveImage() throws IOException {
        if (subject != null) {
            if (oldImageFile != null) {
                fcImage.setSelectedFile(oldImageFile);
            }
            if (fcImage.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                oldImageFile = fcImage.getSelectedFile();
                if (cbShowProcessed.isSelected()) {
                    subject.getFingers().get(0).getProcessedImage().save(fcImage.getSelectedFile().getAbsolutePath());
                } else {
                    subject.getFingers().get(0).getImage().save(fcImage.getSelectedFile().getAbsolutePath());
                }
            }
        }
    }

    private void updateShownImage() {
        if (cbShowProcessed.isSelected()) {
            view.setShownImage(ShownImage.RESULT);
        } else {
            view.setShownImage(ShownImage.ORIGINAL);
        }
    }

    // ===========================================================
    // Package private methods
    // ===========================================================

    void updateStatus(String status) {
        lblInfo.setText(status);
    }

    NSubject getSubject() {
        return subject;
    }

    NFingerScanner getSelectedScanner() {
        return (NFingerScanner) scannerList.getSelectedValue();
    }

    // ===========================================================
    // Protected methods
    // ===========================================================

    @Override
    protected void initGUI() {
        panelMain = new JPanel();
        panelScanners = new JPanel();
        scrollPaneList = new JScrollPane();
        scannerList = new JList();
        panelButtons = new JPanel();
        btnRefresh = new JButton();
        btnScan = new JButton();
        btnCancel = new JButton();
        btnForce = new JButton();
        cbAutomatic = new JCheckBox();
        scrollPane = new JScrollPane();
        panelSouth = new JPanel();
        panelInfo = new JPanel();
        lblInfo = new JLabel();
        panelSave = new JPanel();
        btnIdentifyPatient = new JButton();
        cbShowProcessed = new JCheckBox();

        setLayout(new BorderLayout());

        panelMain.setLayout(new BorderLayout());

        panelScanners.setBorder(BorderFactory.createTitledBorder("Scanners list"));
        panelScanners.setLayout(new BorderLayout());

        scrollPaneList.setPreferredSize(new Dimension(0, 90));

        scannerList.setModel(new DefaultListModel());
        scannerList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        scannerList.setBorder(LineBorder.createBlackLineBorder());
        scrollPaneList.setViewportView(scannerList);

        panelScanners.add(scrollPaneList, BorderLayout.CENTER);

        panelButtons.setLayout(new FlowLayout(FlowLayout.LEADING));

        btnRefresh.setText("Refresh list");
        panelButtons.add(btnRefresh);

        btnScan.setText("Scan");
        panelButtons.add(btnScan);

        btnCancel.setText("Cancel");
        btnCancel.setEnabled(false);
        panelButtons.add(btnCancel);

        btnForce.setText("Force");
        panelButtons.add(btnForce);

        cbAutomatic.setSelected(true);
        cbAutomatic.setText("Scan automatically");
        panelButtons.add(cbAutomatic);

        panelScanners.add(panelButtons, BorderLayout.SOUTH);

        panelMain.add(panelScanners, BorderLayout.NORTH);
        panelMain.add(scrollPane, BorderLayout.CENTER);

        panelSouth.setLayout(new BorderLayout());

        panelInfo.setBorder(new SoftBevelBorder(BevelBorder.LOWERED));
        panelInfo.setLayout(new GridLayout(1, 1));

        lblInfo.setText(" ");
        panelInfo.add(lblInfo);

        panelSouth.add(panelInfo, BorderLayout.NORTH);

        panelSave.setLayout(new FlowLayout(FlowLayout.LEADING));

        btnIdentifyPatient.setText("Identify Patient");
        btnIdentifyPatient.setEnabled(false);
        panelSave.add(btnIdentifyPatient);

        cbShowProcessed.setSelected(true);
        cbShowProcessed.setText("Show processed image");
        panelSave.add(cbShowProcessed);

        panelSouth.add(panelSave, BorderLayout.SOUTH);

        panelMain.add(panelSouth, BorderLayout.SOUTH);

        add(panelMain, BorderLayout.CENTER);

        panelLicensing = new LicensingPanel(requiredLicenses, optionalLicenses);
        add(panelLicensing, java.awt.BorderLayout.NORTH);

        fcImage = new JFileChooser();
        fcImage.setFileFilter(new Utils.ImageFileFilter(NImages.getSaveFileFilter()));
        fcTemplate = new JFileChooser();
        view = new NFingerView();
        view.setShownImage(ShownImage.RESULT);
        view.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent ev) {
                super.mouseClicked(ev);
                if (ev.getButton() == MouseEvent.BUTTON3) {
                    cbShowProcessed.doClick();
                }
            }

        });
        scrollPane.setViewportView(view);

        btnRefresh.addActionListener(this);
        btnScan.addActionListener(this);
        btnCancel.addActionListener(this);
        btnForce.addActionListener(this);
        btnIdentifyPatient.addActionListener(this);
        cbShowProcessed.addActionListener(this);
        scannerList.addListSelectionListener(new ScannerSelectionListener());
    }

    @Override
    protected void setDefaultValues() {
        // No default values.
    }

    @Override
    protected void updateControls() {
        btnScan.setEnabled(!scanning);
        btnCancel.setEnabled(scanning);
        btnForce.setEnabled(scanning && !cbAutomatic.isSelected());
        btnRefresh.setEnabled(!scanning);
        btnIdentifyPatient.setEnabled(!scanning && (subject != null) && (subject.getStatus() == NBiometricStatus.OK));
        cbShowProcessed.setEnabled(!scanning);
        cbAutomatic.setEnabled(!scanning);
    }

    @Override
    protected void updateFingersTools() {
        FingersTools.getInstance().getClient().reset();
        FingersTools.getInstance().getClient().setUseDeviceManager(true);
        FingersTools.getInstance().getClient().setFingersReturnProcessedImage(true);
    }

    // ===========================================================
    // Public methods
    // ===========================================================

    public void updateScannerList() {
        DefaultListModel model = (DefaultListModel) scannerList.getModel();
        model.clear();
        for (NDevice device : deviceManager.getDevices()) {
            model.addElement(device);
        }
        NFingerScanner scanner = (NFingerScanner) FingersTools.getInstance().getClient().getFingerScanner();
        if ((scanner == null) && (model.getSize() > 0)) {
            scannerList.setSelectedIndex(0);
        } else if (scanner != null) {
            scannerList.setSelectedValue(scanner, true);
        }
    }

    public void cancelCapturing() {
        FingersTools.getInstance().getClient().cancel();
    }

    // ===========================================================
    // Event handling
    // ===========================================================

    @Override
    public void actionPerformed(ActionEvent ev) {
        try {
            if (ev.getSource() == btnRefresh) {
                updateScannerList();
            } else if (ev.getSource() == btnScan) {
                startCapturing();
            } else if (ev.getSource() == btnCancel) {
                cancelCapturing();
            } else if (ev.getSource() == btnForce) {
                FingersTools.getInstance().getClient().force();
            } else if (ev.getSource() == btnIdentifyPatient) {
                identifyPatient();
            } else if (ev.getSource() == cbShowProcessed) {
                updateShownImage();
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, e.toString(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ===========================================================
    // Inner classes
    // ===========================================================


    private class CaptureCompletionHandler implements CompletionHandler<NBiometricTask, Object> {

        @Override
        public void completed(final NBiometricTask result, final Object attachment) {
            if(result != null) {
                SwingUtilities.invokeLater(new Runnable() {

                    @Override
                    public void run() {
                        scanning = false;
                        updateShownImage();
                        if (result.getStatus() == NBiometricStatus.OK) {
                            updateStatus("Quality: " + getSubject().getFingers().get(0).getObjects().get(0).getQuality());
                        } else {
                            updateStatus(result.getStatus().toString());
                        }
                        updateControls();
                    }

                });
            }
        }

        @Override
        public void failed(final Throwable th, final Object attachment) {
            SwingUtilities.invokeLater(new Runnable() {

                @Override
                public void run() {
                    th.printStackTrace();
                    scanning = false;
                    updateShownImage();
                    showErrorDialog(th);
                    updateControls();
                }

            });
        }

    }

    private class EnrollCompletionHandler implements CompletionHandler<NBiometricTask, Object> {

        @Override
        public void completed(final NBiometricTask result, final Object attachment) {

        }

        @Override
        public void failed(final Throwable throwable, final Object attachment) {

        }

    }

    private class ScannerSelectionListener implements ListSelectionListener {

        @Override
        public void valueChanged(ListSelectionEvent e) {
            FingersTools.getInstance().getClient().setFingerScanner(getSelectedScanner());
        }

    }

}
