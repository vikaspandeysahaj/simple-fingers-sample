package com.neurotec.samples;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.io.IOException;

public final class MainPanel extends JPanel implements ChangeListener {

    // ===========================================================
    // Private static fields
    // ===========================================================

    private static final long serialVersionUID = 1L;

    // ===========================================================
    // Private fields
    // ===========================================================

    private JTabbedPane tabbedPane;
    //private EnrollFromImage enrollFromImage;
    private EnrollFromScanner enrollFromScanner;
    //private IdentifyFinger identifyFinger;
    //private VerifyFinger verifyFinger;
    //private SegmentFingers segmentFingers;

    // ===========================================================
    // Public constructor
    // ===========================================================

//    @Override
//    public void init() {
//        final Component parent = this;

    public MainPanel() {
        super(new GridLayout(1, 1));
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }
        initGUI();
    }

    // ===========================================================
    // Private methods
    // ===========================================================

    private void initGUI() {
        //JOptionPane.showMessageDialog(this, "initGUI()");
        tabbedPane = new JTabbedPane();
        tabbedPane.addChangeListener(this);
        //JOptionPane.showMessageDialog(this, "tabbedPane created");
//		enrollFromImage = new EnrollFromImage();
//		enrollFromImage.init();
//		tabbedPane.addTab("Enroll from image", enrollFromImage);

        enrollFromScanner = new EnrollFromScanner();
        enrollFromScanner.init();
        tabbedPane.addTab("Enroll from scanner", enrollFromScanner);
       // JOptionPane.showMessageDialog(this, "enrollFromScanner created");

//		identifyFinger = new IdentifyFinger();
//		identifyFinger.init();
//		tabbedPane.addTab("Identify finger", identifyFinger);

//		verifyFinger = new VerifyFinger();
//		verifyFinger.init();
//		tabbedPane.addTab("Verify finger", verifyFinger);

//		segmentFingers = new SegmentFingers();
//		segmentFingers.init();
//		tabbedPane.addTab("Segment fingers", segmentFingers);

        add(tabbedPane);
        setPreferredSize(new Dimension(540, 600));
        tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
    }

    // ===========================================================
    // Public methods
    // ===========================================================

    public void obtainLicenses(BasePanel panel) throws IOException {
        if (!panel.isObtained()) {
            boolean status = FingersTools.getInstance().obtainLicenses(panel.getRequiredLicenses());
            FingersTools.getInstance().obtainLicenses(panel.getOptionalLicenses());
            panel.getLicensingPanel().setRequiredComponents(panel.getRequiredLicenses());
            panel.getLicensingPanel().setOptionalComponents(panel.getOptionalLicenses());
            panel.updateLicensing(status);
        }
    }

    // ===========================================================
    // Event handling
    // ===========================================================

    @Override
    public void stateChanged(ChangeEvent evt) {
        if (evt.getSource() == tabbedPane) {
            try {
                switch (tabbedPane.getSelectedIndex()) {
//				case 0: {
//					obtainLicenses(enrollFromImage);
//					enrollFromImage.updateFingersTools();
//					break;
//				}
                    case 0: {
                        obtainLicenses(enrollFromScanner);
                        enrollFromScanner.updateFingersTools();
                        enrollFromScanner.updateScannerList();
                        break;
                    }
//				case 2: {
//					obtainLicenses(identifyFinger);
//					identifyFinger.updateFingersTools();
//					break;
//				}
//				case 3: {
//					obtainLicenses(verifyFinger);
//					verifyFinger.updateFingersTools();
//					break;
//				}
//				case 4: {
//					obtainLicenses(segmentFingers);
//					segmentFingers.updateFingersTools();
//					break;
//				}
                    default: {
                        throw new IndexOutOfBoundsException("unreachable");
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Could not obtain licenses for components: " + e.toString(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

}
