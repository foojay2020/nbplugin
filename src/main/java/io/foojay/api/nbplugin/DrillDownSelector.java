/*
 * Copyright (c) 2020 by Gerrit Grunwald
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.foojay.api.nbplugin;

import io.foojay.api.discoclient.DiscoClient;
import io.foojay.api.discoclient.pkg.Pkg;
import io.foojay.api.discoclient.pkg.Distribution;
import io.foojay.api.discoclient.pkg.ArchiveType;
import io.foojay.api.discoclient.pkg.MajorVersion;
import io.foojay.api.discoclient.pkg.SemVer;
import io.foojay.api.discoclient.util.PkgInfo;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicProgressBarUI;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Rectangle2D.Double;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public class DrillDownSelector extends JPanel {
    private static final Color                        DOWNLOAD_AREA_STD      = new Color(28, 107, 177);
    private static final Color                        DOWNLOAD_AREA_HOVER    = new Color(4, 124, 192);
    private static final Color                        DOWNLOAD_AREA_DISABLED = new Color(128, 128, 128);
    private static final Color                        DISABLED_LABEL_COLOR   = new Color(190, 190, 190);
    private static final Color                        PROGRESS_BAR_TRACK     = new Color(21, 82, 134);
    private static final Color                        BACKGROUND_COLOR       = new Color(45, 45, 45);
    private static final Color                        TEXT_COLOR             = new Color(164, 164, 164);
    private              DiscoClient                  discoClient;
    private              JLabel                       osLabel;
    private              ButtonGroup                  buttonGroup;
    private              Map<Integer, JRadioButton>   jdkSelectors;
    private              RJPanel                      downloadArea;
    private              JLabel                       downloadLabel;
    private              JLabel                       versionNumberLabel;
    private              JLabel                       fileNameLabel;
    private              JFileChooser                 directoryChooser;
    private              JProgressBar                 progressBar;
    private              JComboBox<ArchiveType>       extensionComboBox;
    private              Map<String, PkgInfo>         pkgMap;
    private              java.util.List<Pkg>          pkgsFound8;
    private              java.util.List<Pkg>          pkgsFoundLastLts;
    private              List<Pkg>                    pkgsFoundCurrent;
    private              JCheckBox               detailsCheckBox;
    private              JComboBox<SemVer>       semVerComboBox;
    private              JComboBox<Distribution> distributionsComboBox;
    private              JComboBox<ArchiveType>       extensionsComboBox;


    public DrillDownSelector() {
        init();
        registerListeners();

        updatePkgMap(Distribution.ZULU);
    }


    private void init() {
        setPreferredSize(new Dimension(400, 300));

        discoClient            = new DiscoClient();
        progressBar            = new JProgressBar(0, 100);
        progressBar.setPreferredSize(new Dimension(progressBar.getPreferredSize().width, 5));
        progressBar.setForeground(Color.WHITE);
        progressBar.setValue(0);
        progressBar.setUI(new DrillDownSelector.FlatProgressUI());
        progressBar.setVisible(false);
        extensionComboBox = new JComboBox<>();
        extensionComboBox.setEnabled(false);
        extensionComboBox.setMaximumSize(new Dimension(80, extensionComboBox.getPreferredSize().height));
        extensionComboBox.addActionListener(e -> {

        });
        pkgMap           = new HashMap<>();
        pkgsFound8       = new ArrayList<>();
        pkgsFoundLastLts = new ArrayList<>();
        pkgsFoundCurrent = new ArrayList<>();

        directoryChooser = new JFileChooser();
        directoryChooser.setCurrentDirectory(new File("."));
        directoryChooser.setDialogTitle("Select destination folder");
        directoryChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        directoryChooser.setAcceptAllFileFilterUsed(false);

        osLabel = new JLabel("Download for " + discoClient.getOperatingSystem().getUiString());
        osLabel.setForeground(TEXT_COLOR);

        buttonGroup  = new ButtonGroup();
        jdkSelectors = new LinkedHashMap<>();
        List<MajorVersion> maintainedMajorVersions = discoClient.getMaintainedMajorVersions();
        for (MajorVersion majorVersion : maintainedMajorVersions) {
            jdkSelectors.put(majorVersion.getAsInt(), createRadioButton(majorVersion, buttonGroup));
        }

        downloadLabel = new JLabel("Download");
        downloadLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        downloadLabel.setForeground(DISABLED_LABEL_COLOR);
        downloadLabel.setFont(new Font(downloadLabel.getFont().getName(), Font.BOLD, 16));

        versionNumberLabel = new JLabel("-");
        versionNumberLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        versionNumberLabel.setForeground(DISABLED_LABEL_COLOR);

        fileNameLabel = new JLabel("-");
        fileNameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        fileNameLabel.setForeground(DISABLED_LABEL_COLOR);

        Box extensionBox = Box.createHorizontalBox();
        extensionBox.add(versionNumberLabel);
        extensionBox.add(Box.createRigidArea(new Dimension(50, 0)));
        extensionBox.add(extensionComboBox);

        Box downloadVBox = Box.createVerticalBox();
        downloadVBox.add(downloadLabel);
        downloadVBox.add(Box.createRigidArea(new Dimension(0, 10)));
        downloadVBox.add(extensionBox);
        downloadVBox.add(Box.createRigidArea(new Dimension(0, 10)));
        downloadVBox.add(fileNameLabel);
        downloadVBox.add(Box.createRigidArea(new Dimension(0, 10)));
        downloadVBox.add(progressBar);
        downloadVBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));

        downloadArea = new RJPanel(20, 20);
        downloadArea.setMaximumSize(new Dimension(350, 120));
        downloadArea.setPreferredSize(new Dimension(350, 120));
        downloadArea.setEnabled(false);
        downloadArea.setBackground(DOWNLOAD_AREA_DISABLED);
        downloadArea.add(downloadVBox);
        downloadArea.setAlignmentX(Component.LEFT_ALIGNMENT);

        Box vBox = Box.createVerticalBox();
        vBox.add(osLabel);
        vBox.add(Box.createRigidArea(new Dimension(0, 10)));


        Box leftVBox  = Box.createVerticalBox();
        Box rightVBox = Box.createVerticalBox();
        jdkSelectors.entrySet().forEach(entry -> {
            if (entry.getKey() < 13) {
                leftVBox.add(entry.getValue());
                leftVBox.add(Box.createRigidArea(new Dimension(0, 10)));
            } else {
                rightVBox.add(entry.getValue());
                rightVBox.add(Box.createRigidArea(new Dimension(0, 10)));
            }
        });

        Box selectorBox = Box.createHorizontalBox();
        selectorBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        selectorBox.setAlignmentY(Component.TOP_ALIGNMENT);
        selectorBox.add(leftVBox);
        selectorBox.add(Box.createRigidArea(new Dimension(30, 0)));
        selectorBox.add(rightVBox);

        selectorBox.add(Box.createRigidArea(new Dimension(30, 0)));
        detailsCheckBox = new JCheckBox("Details");
        detailsCheckBox.setForeground(TEXT_COLOR);
        detailsCheckBox.setVisible(false);
        selectorBox.add(detailsCheckBox);

        selectorBox.add(Box.createRigidArea(new Dimension(30, 0)));
        semVerComboBox = new JComboBox<>();
        semVerComboBox.setRenderer(new SemVerListCellRenderer());
        semVerComboBox.setVisible(false);
        selectorBox.add(semVerComboBox);

        selectorBox.add(Box.createRigidArea(new Dimension(30, 0)));
        distributionsComboBox = new JComboBox<>();
        distributionsComboBox.setRenderer(new DistributionListCellRenderer());
        distributionsComboBox.setVisible(false);
        selectorBox.add(distributionsComboBox);

        selectorBox.add(Box.createRigidArea(new Dimension(30, 0)));
        extensionsComboBox = new JComboBox<>();
        extensionsComboBox.setVisible(false);
        selectorBox.add(extensionsComboBox);

        vBox.add(selectorBox);
        vBox.add(Box.createRigidArea(new Dimension(0, 10)));
        vBox.add(downloadArea);
        vBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        vBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        vBox.setAlignmentY(Component.TOP_ALIGNMENT);

        add(vBox);

        setBorder(new EmptyBorder(10, 10, 10 , 10));

        setBackground(BACKGROUND_COLOR);
    }

    private void registerListeners() {
        discoClient.setOnDCEvent(e -> {
            switch(e.getType()) {
                case DOWNLOAD_STARTED :
                    SwingUtilities.invokeLater(() -> {
                        progressBar.setVisible(true);
                        jdkSelectors.values().forEach(radioButton -> radioButton.setEnabled(false));
                    });
                    break;
                case DOWNLOAD_PROGRESS:
                    SwingUtilities.invokeLater(() -> progressBar.setValue((int) ((double) e.getFraction() / (double) e.getFileSize() * 100)));
                    break;
                case DOWNLOAD_FINISHED:
                    SwingUtilities.invokeLater(() -> {
                        progressBar.setValue(0);
                        progressBar.setVisible(false);
                        jdkSelectors.values().forEach(radioButton -> radioButton.setEnabled(true));
                    });
                    break;
            }
        });
        detailsCheckBox.addActionListener(e -> {
            if (detailsCheckBox.isSelected()) {
                semVerComboBox.setVisible(true);
            } else {
                semVerComboBox.setVisible(false);
                distributionsComboBox.setVisible(false);
                extensionsComboBox.setVisible(false);
            }
        });
        semVerComboBox.addActionListener(e -> {
            updateDistros((SemVer) semVerComboBox.getSelectedItem());
        });
        downloadArea.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(final MouseEvent e) {
                if (downloadArea.isEnabled()) {
                    SwingUtilities.invokeLater(() -> downloadArea.setBackground(DOWNLOAD_AREA_HOVER));

                    final Integer selectedFeatureVersion = jdkSelectors.entrySet().stream().filter(entry -> entry.getValue().isSelected()).mapToInt(entry -> entry.getKey()).findFirst().getAsInt();
                    downloadPkg(getParent(), selectedFeatureVersion);
                }
            }
            @Override public void mouseReleased(final MouseEvent e) {
                if (downloadArea.isEnabled()) {
                    SwingUtilities.invokeLater(() -> downloadArea.setBackground(DOWNLOAD_AREA_STD));
                }
            }
            @Override public void mouseEntered(final MouseEvent e) {
                if (downloadArea.isEnabled()) {
                    SwingUtilities.invokeLater(() -> {
                        downloadArea.setBackground(DOWNLOAD_AREA_HOVER);
                        downloadArea.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    });
                }
            }
            @Override public void mouseExited(final MouseEvent e) {
                if (downloadArea.isEnabled()) {
                    SwingUtilities.invokeLater(() -> {
                        downloadArea.setBackground(DOWNLOAD_AREA_STD);
                        downloadArea.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                    });
                }
            }
        });
    }

    private JRadioButton createRadioButton(final MajorVersion majorVersion, final ButtonGroup buttonGroup) {
        JRadioButton radioButton = new JRadioButton("JDK " + majorVersion.getAsInt());
        radioButton.setForeground(TEXT_COLOR);
        radioButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        radioButton.addActionListener(e -> updateVersions(majorVersion.getAsInt()));
        buttonGroup.add(radioButton);
        return radioButton;
    }

    private void updateVersions(final Integer featureVersion) {
        SwingUtilities.invokeLater(() -> detailsCheckBox.setVisible(true));

        List<SemVer> versionNumbers = discoClient.getMajorVersion(featureVersion, false).getVersions();
        SwingUtilities.invokeLater(() -> {
            semVerComboBox.removeAllItems();
            versionNumbers.forEach(versionNumber -> semVerComboBox.addItem(versionNumber));
        });

        downloadArea.setEnabled(true);
        downloadArea.setBackground(DOWNLOAD_AREA_STD);
        downloadLabel.setForeground(Color.WHITE);
        versionNumberLabel.setForeground(Color.WHITE);
        fileNameLabel.setForeground(Color.WHITE);

        extensionComboBox.removeAllItems();
        extensionComboBox.setEnabled(extensionComboBox.getItemCount() != 0);

        final PkgInfo selectedPkgInfo = pkgMap.get(featureVersion);
        //versionNumberLabel.setText(selectedPkgInfo.getVersionNumber().toString());
        //fileNameLabel.setText(selectedPkgInfo.getFileName());
    }

    private void updateDistros(final SemVer semVer) {
        List<Distribution> distributions = discoClient.getDistributionsThatSupportSemVer(semVer);
        SwingUtilities.invokeLater(() -> {
            distributionsComboBox.setVisible(true);
            distributionsComboBox.removeAllItems();
            distributions.forEach(distribution -> distributionsComboBox.addItem(distribution));
        });
    }

    private void downloadPkg(final Container parent, final Integer featureVersion) {
        if (!downloadArea.isEnabled() || progressBar.isVisible() || null == pkgMap.get(featureVersion)) { return; }

        String targetFolder;
        if (directoryChooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
            targetFolder = directoryChooser.getSelectedFile().getAbsolutePath();
        } else {
            return;
        }

        if (null != targetFolder) {
            PkgInfo selectedPkgInfo = pkgMap.get(featureVersion);
            String  fileName        = selectedPkgInfo.getFileName();
            discoClient.downloadPkg(selectedPkgInfo, targetFolder + File.separator + fileName);
        }
    }

    private void updatePkgMap(final Distribution distribution) {
        extensionComboBox.removeAllItems();
        extensionComboBox.setEnabled(false);
        pkgsFound8.clear();
        pkgsFoundLastLts.clear();
        pkgsFoundCurrent.clear();
        pkgMap.clear();


        buttonGroup.clearSelection();
        downloadArea.setEnabled(false);
        downloadArea.setBackground(DOWNLOAD_AREA_DISABLED);
        downloadLabel.setForeground(DISABLED_LABEL_COLOR);
        versionNumberLabel.setForeground(DISABLED_LABEL_COLOR);
        fileNameLabel.setForeground(DISABLED_LABEL_COLOR);
        versionNumberLabel.setText("-");
        fileNameLabel.setText("-");
    }

    private void updateSelectedPkg(final SemVer semVer, final Pkg pkg) {
        pkgMap.put(semVer.toString(), discoClient.getPkgInfo(pkg.getId(), pkg.getJavaVersion()));
        final PkgInfo selectedPkgInfo = pkgMap.get(semVer.toString());
        versionNumberLabel.setText(selectedPkgInfo.getJavaVersion().toString());
        fileNameLabel.setText(selectedPkgInfo.getFileName());
    }

    private Distribution showDistributionDialog(final Container parent) {
        Distribution[] distributions = Distribution.getDistributions().toArray(new Distribution[0]);
        Distribution distribution = (Distribution) JOptionPane.showInputDialog(parent, "Choose a dsitribution", "Distributions",
                                                                               JOptionPane.PLAIN_MESSAGE, null, distributions, Distribution.ZULU);
        return null == distribution ? Distribution.ZULU : distribution;
    }


    // ******************** Inner classes *************************************
    class RJPanel extends JPanel {
        private int arcWidth;
        private int archHeight;

        public RJPanel() {
            this(0, 0);
        }
        public RJPanel(final int arcWidth, final int archHeight) {
            super();
            this.arcWidth   = arcWidth;
            this.archHeight = archHeight;
            setBorder(new EmptyBorder(0,0,0,0));
        }


        public int getArcWidth() { return arcWidth; }
        public void setArcWidth(final int archWidth) {
            this.arcWidth = archWidth;
            repaint();
        }

        public int getArchHeight() { return archHeight; }
        public void setArchHeight(final int archHeight) {
            this.archHeight = archHeight;
            repaint();
        }

        @Override public boolean isOpaque() { return false; }

        @Override public void paintComponent(final Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            super.paintComponent(g2);

            Insets insets = getInsets();
            int x = insets.left;
            int y = insets.top;
            int w = getWidth() - insets.left - insets.right;
            int h = getHeight() - insets.top - insets.bottom;
            g2.setColor(getBackground());
            g2.fillRoundRect(x, y, w, h, arcWidth, archHeight);
        }
    }

    class FlatProgressUI extends BasicProgressBarUI {
        Rectangle rect = new Rectangle();

        @Override protected void paintDeterminate(final Graphics g, final JComponent c) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width  = progressBar.getWidth();
            int height = progressBar.getHeight();
            double progress = progressBar.getPercentComplete();
            if (progress < 0) {
                progress = 0;
            } else if (progress > 1) {
                progress = 1;
            }
            g2.setColor(PROGRESS_BAR_TRACK);
            g2.fillRect(0, 0, width, height);

            g2.setColor(progressBar.getForeground());
            Rectangle2D r = new Double(0, 0, width * progress, height);
            g2.fill(r);
            g2.dispose();
        }

        @Override protected void paintIndeterminate(final Graphics g, final JComponent c) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            rect = getBox(rect);
            g2.setColor(progressBar.getForeground());
            g2.fillRect(rect.x, rect.y, rect.width, rect.height);
        }
    }
}
