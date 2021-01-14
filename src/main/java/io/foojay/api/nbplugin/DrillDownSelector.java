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
import io.foojay.api.discoclient.pkg.Architecture;
import io.foojay.api.discoclient.pkg.ArchiveType;
import io.foojay.api.discoclient.pkg.Bitness;
import io.foojay.api.discoclient.pkg.Distribution;
import io.foojay.api.discoclient.pkg.Latest;
import io.foojay.api.discoclient.pkg.MajorVersion;
import io.foojay.api.discoclient.pkg.PackageType;
import io.foojay.api.discoclient.pkg.Pkg;
import io.foojay.api.discoclient.pkg.ReleaseStatus;
import io.foojay.api.discoclient.pkg.Scope;
import io.foojay.api.discoclient.pkg.SemVer;
import io.foojay.api.discoclient.pkg.TermOfSupport;
import io.foojay.api.discoclient.pkg.VersionNumber;
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
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static javax.swing.Box.createGlue;


public class DrillDownSelector extends JPanel {
    private static final Color                      DOWNLOAD_AREA_STD      = new Color(28, 107, 177);
    private static final Color                      DOWNLOAD_AREA_HOVER    = new Color(4, 124, 192);
    private static final Color                      DOWNLOAD_AREA_DISABLED = new Color(128, 128, 128);
    private static final Color                      DISABLED_LABEL_COLOR   = new Color(190, 190, 190);
    private static final Color                      PROGRESS_BAR_TRACK     = new Color(21, 82, 134);
    private static final Color                      BACKGROUND_COLOR       = new Color(45, 45, 45);
    private static final Color                      TEXT_COLOR             = new Color(164, 164, 164);
    private              DiscoClient                discoClient;
    private              JLabel                     osLabel;
    private              ButtonGroup                buttonGroup;
    private              Map<Integer, JRadioButton> jdkSelectors;
    private              RJPanel                    downloadArea;
    private              JLabel                     downloadLabel;
    private              JLabel                     versionNumberLabel;
    private              JLabel                     fileNameLabel;
    private              JFileChooser               directoryChooser;
    private              JProgressBar               progressBar;
    private              List<Pkg>                  pkgsFound;
    private              SemVer                     selectedSemVer;
    private              Distribution               selectedDistribution;
    private              ArchiveType                selectedArchiveType;
    private              Pkg                        selectedPkg;
    private              JCheckBox                  detailsCheckBox;
    private              JComboBox<SemVer>          semVerComboBox;
    private              JComboBox<Distribution>    distributionsComboBox;
    private              JComboBox<ArchiveType>     archiveTypeComboBox;


    public DrillDownSelector() {
        init();
        registerListeners();
    }


    private void init() {
        setPreferredSize(new Dimension(600, 300));

        discoClient            = new DiscoClient();
        progressBar            = new JProgressBar(0, 100);
        progressBar.setPreferredSize(new Dimension(progressBar.getPreferredSize().width, 5));
        progressBar.setForeground(Color.WHITE);
        progressBar.setValue(0);
        progressBar.setUI(new DrillDownSelector.FlatProgressUI());
        progressBar.setVisible(false);

        pkgsFound            = new ArrayList<>();
        selectedSemVer       = null;
        selectedDistribution = null;
        selectedArchiveType  = null;
        selectedPkg          = null;

        directoryChooser = new JFileChooser();
        directoryChooser.setCurrentDirectory(new File("."));
        directoryChooser.setDialogTitle("Select destination folder");
        directoryChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        directoryChooser.setAcceptAllFileFilterUsed(false);

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


        Box downloadVBox = Box.createVerticalBox();
        downloadVBox.add(downloadLabel);
        downloadVBox.add(Box.createRigidArea(new Dimension(0, 10)));
        downloadVBox.add(versionNumberLabel);
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

        selectorBox.add(Box.createRigidArea(new Dimension(50, 0)));
        detailsCheckBox = new JCheckBox("Details");
        detailsCheckBox.setForeground(TEXT_COLOR);
        detailsCheckBox.setVisible(false);
        selectorBox.add(detailsCheckBox);

        Box drillDownBox = Box.createVerticalBox();
        drillDownBox.setAlignmentX(Component.RIGHT_ALIGNMENT);
        drillDownBox.setAlignmentY(Component.TOP_ALIGNMENT);
        drillDownBox.setMaximumSize(new Dimension(200, 100));

        semVerComboBox = new JComboBox<>();
        semVerComboBox.setRenderer(new SemVerListCellRenderer());
        semVerComboBox.setVisible(false);
        drillDownBox.add(semVerComboBox);

        drillDownBox.add(Box.createRigidArea(new Dimension(0, 10)));
        distributionsComboBox = new JComboBox<>();
        distributionsComboBox.setRenderer(new DistributionListCellRenderer());
        distributionsComboBox.setVisible(false);
        drillDownBox.add(distributionsComboBox);

        drillDownBox.add(Box.createRigidArea(new Dimension(0, 10)));
        archiveTypeComboBox = new JComboBox<>();
        archiveTypeComboBox.setVisible(false);
        drillDownBox.add(archiveTypeComboBox);

        Box vBox = Box.createVerticalBox();
        vBox.add(selectorBox);
        vBox.add(Box.createRigidArea(new Dimension(0, 10)));
        vBox.add(downloadArea);
        vBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        vBox.setAlignmentY(Component.TOP_ALIGNMENT);

        Box hBox = Box.createHorizontalBox();
        hBox.add(vBox);
        hBox.add(Box.createRigidArea(new Dimension(30, 0)));
        hBox.add(drillDownBox);
        hBox.setAlignmentX(Component.CENTER_ALIGNMENT);
        hBox.setAlignmentY(Component.TOP_ALIGNMENT);

        osLabel = new JLabel("Download for " + discoClient.getOperatingSystem().getUiString());
        osLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        osLabel.setForeground(TEXT_COLOR);
        osLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        Box mainVBox = Box.createVerticalBox();
        mainVBox.add(createGlue());
        mainVBox.add(osLabel);
        mainVBox.add(Box.createRigidArea(new Dimension(0, 10)));
        mainVBox.add(hBox);

        add(mainVBox);

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
                archiveTypeComboBox.setVisible(false);
            }
        });
        semVerComboBox.addActionListener(e -> {
            if (null == semVerComboBox.getSelectedItem()) {
                selectedSemVer = null;
                return;
            }
            distributionsComboBox.setVisible(false);
            archiveTypeComboBox.setVisible(false);
            pkgsFound.clear();
            selectedPkg    = null;
            selectedSemVer = (SemVer) semVerComboBox.getSelectedItem();
            updateDistros();
        });
        distributionsComboBox.addActionListener(e -> {
            if (null == distributionsComboBox.getSelectedItem()) {
                selectedDistribution = null;
                return;
            }
            archiveTypeComboBox.setVisible(false);
            pkgsFound.clear();
            selectedPkg          = null;
            selectedDistribution = (Distribution) distributionsComboBox.getSelectedItem();
            updateArchiveTypes();
        });
        archiveTypeComboBox.addActionListener(e -> {
            if (null == archiveTypeComboBox.getSelectedItem()) {
                selectedArchiveType = null;
                return;
            }
            selectedPkg         = null;
            selectedArchiveType = (ArchiveType) archiveTypeComboBox.getSelectedItem();
            updateSelectedPkg();
        });
        downloadArea.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(final MouseEvent e) {
                if (downloadArea.isEnabled()) {
                    SwingUtilities.invokeLater(() -> downloadArea.setBackground(DOWNLOAD_AREA_HOVER));
                    downloadPkg(getParent());
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
        List<SemVer> versionNumbers = discoClient.getMajorVersion(featureVersion, false).getVersions();
        SwingUtilities.invokeLater(() -> {
            detailsCheckBox.setVisible(true);
            semVerComboBox.removeAllItems();
            versionNumbers.forEach(versionNumber -> semVerComboBox.addItem(versionNumber));
            semVerComboBox.setSelectedIndex(-1);
        });

        VersionNumber vn = versionNumbers.stream().max(Comparator.comparing(SemVer::getVersionNumber)).get().getVersionNumber();
        List<Pkg> pkgs = discoClient.getPkgs(Distribution.ZULU, vn, Latest.PER_VERSION, discoClient.getOperatingSystem(), Architecture.NONE, Bitness.NONE, ArchiveType.NONE, PackageType.JDK, null,ReleaseStatus.GA, TermOfSupport.NONE, Scope.PUBLIC);
        if (pkgs.isEmpty()) { return; }
        selectedPkg = pkgs.get(0);
        enableDownloadArea();

        archiveTypeComboBox.removeAllItems();
    }

    private void updateDistros() {
        if (null == selectedSemVer) { return; }
        List<Distribution> distributions = discoClient.getDistributionsThatSupportSemVer(selectedSemVer);
        SwingUtilities.invokeLater(() -> {
            if (null != semVerComboBox.getSelectedItem() && semVerComboBox.isVisible()) {
                distributionsComboBox.setVisible(true);
            }
            distributionsComboBox.removeAllItems();
            distributions.forEach(distribution -> distributionsComboBox.addItem(distribution));
        });
    }

    private void updateArchiveTypes() {
        if (null == selectedDistribution || null == selectedSemVer) { return; }
        pkgsFound = discoClient.getPkgs(selectedDistribution, VersionNumber.fromText(selectedSemVer.toString()), Latest.NONE,
                                        discoClient.getOperatingSystem(), Architecture.NONE, Bitness.NONE, ArchiveType.NONE, PackageType.JDK,
                                        Boolean.TRUE, ReleaseStatus.GA, TermOfSupport.NONE, Scope.PUBLIC);

        Set<ArchiveType> archiveTypes = new HashSet<>();
        pkgsFound.forEach(pkg -> archiveTypes.add(pkg.getArchiveType()));
        if (distributionsComboBox.isVisible() && null != selectedDistribution) {
            archiveTypeComboBox.setVisible(true);
        }
        archiveTypeComboBox.removeAllItems();
        archiveTypes.forEach(archiveType -> archiveTypeComboBox.addItem(archiveType));
        archiveTypeComboBox.setSelectedIndex(-1);
        if (archiveTypes.isEmpty()) {
            archiveTypeComboBox.setVisible(false);
            selectedPkg = null;
            disableDownloadArea();
            archiveTypeComboBox.setSelectedItem(null);
        }
        archiveTypeComboBox.setVisible(!archiveTypes.isEmpty());
        enableDownloadArea();
    }

    private void downloadPkg(final Container parent) {
        if (!downloadArea.isEnabled() || progressBar.isVisible()) { return; }

        String targetFolder;
        if (directoryChooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
            targetFolder = directoryChooser.getSelectedFile().getAbsolutePath();
        } else {
            return;
        }

        if (null != selectedPkg && null != targetFolder) {
            PkgInfo pkgInfo  = discoClient.getPkgInfo(selectedPkg.getEphemeralId(), selectedPkg.getJavaVersion());
            String  fileName = selectedPkg.getFileName();
            discoClient.downloadPkg(pkgInfo, targetFolder + File.separator + fileName);
        }
    }

    private void enableDownloadArea() {
        if (null == selectedPkg) { return; }
        downloadArea.setEnabled(false);
        downloadArea.setBackground(DOWNLOAD_AREA_STD);
        downloadLabel.setForeground(Color.WHITE);
        versionNumberLabel.setForeground(Color.WHITE);
        fileNameLabel.setForeground(Color.WHITE);
        versionNumberLabel.setText(selectedPkg.getJavaVersion().toString());
        fileNameLabel.setText(selectedPkg.getFileName());
    }
    private void disableDownloadArea() {
        downloadArea.setEnabled(false);
        downloadArea.setBackground(DOWNLOAD_AREA_DISABLED);
        downloadLabel.setForeground(DISABLED_LABEL_COLOR);
        versionNumberLabel.setForeground(DISABLED_LABEL_COLOR);
        fileNameLabel.setForeground(DISABLED_LABEL_COLOR);
        versionNumberLabel.setText("-");
        fileNameLabel.setText("-");
    }

    private void updateSelectedPkg() {
        if (null == archiveTypeComboBox.getSelectedItem()) { return; }
        Optional<Pkg> selectedPkgOpt = pkgsFound.stream()
                                                .filter(pkg -> pkg.getJavaVersion().compareTo(selectedSemVer) == 0)
                                                .filter(pkg -> pkg.getDistribution().getUiString().equals(selectedDistribution.getUiString()))
                                                .filter(pkg -> pkg.getArchiveType().getUiString().equals(selectedArchiveType.getUiString()))
                                                .findFirst();
        if (selectedPkgOpt.isPresent()) {
            selectedPkg = selectedPkgOpt.get();
            enableDownloadArea();
        } else {
            disableDownloadArea();
        }
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
