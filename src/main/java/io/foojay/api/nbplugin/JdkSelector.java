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
import io.foojay.api.discoclient.pkg.Bitness;
import io.foojay.api.discoclient.pkg.Pkg;
import io.foojay.api.discoclient.pkg.PackageType;
import io.foojay.api.discoclient.pkg.Distribution;
import io.foojay.api.discoclient.pkg.ArchiveType;
import io.foojay.api.discoclient.pkg.Latest;
import io.foojay.api.discoclient.pkg.MajorVersion;
import io.foojay.api.discoclient.pkg.ReleaseStatus;
import io.foojay.api.discoclient.pkg.Scope;
import io.foojay.api.discoclient.pkg.TermOfSupport;
import io.foojay.api.discoclient.pkg.VersionNumber;
import io.foojay.api.discoclient.util.PkgInfo;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicProgressBarUI;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.TextAttribute;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Rectangle2D.Double;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;


public class JdkSelector extends JPanel {
    private static final Color                        DOWNLOAD_AREA_STD      = new Color(28, 107, 177);
    private static final Color                        DOWNLOAD_AREA_HOVER    = new Color(4, 124, 192);
    private static final Color                        DOWNLOAD_AREA_DISABLED = new Color(128, 128, 128);
    private static final Color                        DISABLED_LABEL_COLOR   = new Color(190, 190, 190);
    private static final Color                        PROGRESS_BAR_TRACK     = new Color(21, 82, 134);
    private static final Color                        BACKGROUND_COLOR       = new Color(45, 45, 45);
    private static final Color                        TEXT_COLOR             = new Color(164, 164, 164);
    private              DiscoClient                  discoClient;
    private              int                          selectedFeatureVersion;
    private              MajorVersion                      jdk8;
    private              MajorVersion                      lastLts;
    private              MajorVersion                      current;
    private              JLabel                       osLabel;
    private              ButtonGroup                  buttonGroup;
    private              Map<Integer, JRadioButton>   jdkSelectors;
    private              JLabel                       distributionLabel;
    private              RJPanel                      downloadArea;
    private              JLabel                       downloadLabel;
    private              JLabel                       versionNumberLabel;
    private              JLabel                       fileNameLabel;
    private              List<Pkg>                    pkgs;
    private              JFileChooser                 directoryChooser;
    private              JProgressBar                 progressBar;
    private              JComboBox<ArchiveType>       archiveTypeComboBox;
    private              Map<Integer, PkgInfo>        pkgMap;
    private              List<Pkg>                    pkgsFound8;
    private              List<Pkg>                    pkgsFoundLastLts;
    private              List<Pkg>                    pkgsFoundCurrent;


    public JdkSelector() {
        init();
        registerListeners();

        updatePkgMap(Distribution.ZULU, 8, Integer.valueOf(lastLts.getAsInt()), Integer.valueOf(current.getAsInt()));
    }


    private void init() {
        setPreferredSize(new Dimension(400, 300));

        discoClient            = new DiscoClient();
        pkgs                = new ArrayList<>();
        progressBar            = new JProgressBar(0, 100);
        progressBar.setPreferredSize(new Dimension(progressBar.getPreferredSize().width, 5));
        progressBar.setForeground(Color.WHITE);
        progressBar.setValue(0);
        progressBar.setUI(new FlatProgressUI());
        progressBar.setVisible(false);
        archiveTypeComboBox = new JComboBox<>();
        archiveTypeComboBox.setEnabled(false);
        archiveTypeComboBox.setMaximumSize(new Dimension(100, archiveTypeComboBox.getPreferredSize().height));
        archiveTypeComboBox.addActionListener(e -> {
            if (selectedFeatureVersion == 8) {
                Optional<Pkg> selectedPkg = pkgsFound8.stream().filter(pkg -> pkg.getArchiveType().equals(archiveTypeComboBox.getSelectedItem())).findFirst();
                if (selectedPkg.isPresent()) { updateSelectedPkg(selectedFeatureVersion, selectedPkg.get());}
            } else if (selectedFeatureVersion == lastLts.getAsInt()) {
                Optional<Pkg> selectedPkg = pkgsFoundLastLts.stream().filter(pkg -> pkg.getArchiveType().equals(archiveTypeComboBox.getSelectedItem())).findFirst();
                if (selectedPkg.isPresent()) { updateSelectedPkg(selectedFeatureVersion, selectedPkg.get());}
            } else if (selectedFeatureVersion == current.getAsInt()) {
                Optional<Pkg> selectedPkg = pkgsFoundCurrent.stream().filter(pkg -> pkg.getArchiveType().equals(archiveTypeComboBox.getSelectedItem())).findFirst();
                if (selectedPkg.isPresent()) { updateSelectedPkg(selectedFeatureVersion, selectedPkg.get());}
            }
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

        jdk8    = discoClient.getMajorVersion("8");
        lastLts = discoClient.getLatestLts(false);
        current = discoClient.getLatestSts(false);

        buttonGroup  = new ButtonGroup();
        jdkSelectors = new ConcurrentHashMap<>();
        jdkSelectors.put(8, createRadioButton(jdk8, buttonGroup));
        jdkSelectors.put(lastLts.getAsInt(), createRadioButton(lastLts, buttonGroup));
        if (!current.getVersionNumber().equals(lastLts.getVersionNumber())) {
            jdkSelectors.put(current.getAsInt(), createRadioButton(current, buttonGroup));
        }

        distributionLabel = new JLabel("Distribution");
        Font distributionLabelFont = distributionLabel.getFont();
        distributionLabelFont = new Font(distributionLabelFont.getName(), Font.PLAIN, 9);
        Map attr = distributionLabelFont.getAttributes();
        attr.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
        distributionLabel.setForeground(new Color(128, 128, 128));
        distributionLabel.setFont(distributionLabelFont.deriveFont(attr));

        downloadLabel =  new JLabel("Download");
        downloadLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        downloadLabel.setForeground(DISABLED_LABEL_COLOR);
        downloadLabel.setFont(new Font(downloadLabel.getFont().getName(), Font.BOLD, 16));

        versionNumberLabel = new JLabel("-");
        versionNumberLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        versionNumberLabel.setForeground(DISABLED_LABEL_COLOR);

        fileNameLabel = new JLabel("-");
        fileNameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        fileNameLabel.setForeground(DISABLED_LABEL_COLOR);
        fileNameLabel.setFont(new Font(fileNameLabel.getFont().getName(), Font.PLAIN, 11));

        Box fileInfoBox = Box.createHorizontalBox();
        fileInfoBox.add(versionNumberLabel);
        fileInfoBox.add(Box.createRigidArea(new Dimension(50, 0)));
        fileInfoBox.add(archiveTypeComboBox);

        Box downloadVBox = Box.createVerticalBox();
        downloadVBox.add(downloadLabel);
        downloadVBox.add(Box.createRigidArea(new Dimension(0, 10)));
        downloadVBox.add(fileInfoBox);
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
        jdkSelectors.values().forEach(radioButton -> {
            vBox.add(radioButton);
            vBox.add(Box.createRigidArea(new Dimension(0, 10)));
        });
        vBox.add(Box.createRigidArea(new Dimension(0, 10)));
        vBox.add(distributionLabel);
        vBox.add(Box.createRigidArea(new Dimension(0, 10)));
        vBox.add(downloadArea);
        vBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        vBox.setAlignmentX(Component.LEFT_ALIGNMENT);

        add(vBox);
        setBorder(new EmptyBorder(10, 10, 10 ,10));

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

        distributionLabel.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(final MouseEvent e) {
                if (progressBar.isVisible()) { return; }
                Distribution distribution = showDistributionDialog(getParent());
                updatePkgMap(distribution, 8, Integer.valueOf(lastLts.getAsInt()), Integer.valueOf(current.getAsInt()));
            }
            @Override public void mouseEntered(final MouseEvent e) {
                if (downloadArea.isEnabled()) {
                    SwingUtilities.invokeLater(() -> distributionLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)));
                }
            }
            @Override public void mouseExited(final MouseEvent e) {
                if (downloadArea.isEnabled()) {
                    SwingUtilities.invokeLater(() -> distributionLabel.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR)));
                }
            }
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
        radioButton.addActionListener(e -> updateDownloadArea(majorVersion.getAsInt()));
        buttonGroup.add(radioButton);
        return radioButton;
    }

    private void updateDownloadArea(final Integer featureVersion) {
        if (null == featureVersion || !pkgMap.keySet().contains(featureVersion)) { return; }
        selectedFeatureVersion = featureVersion;
        downloadArea.setEnabled(true);
        downloadArea.setBackground(DOWNLOAD_AREA_STD);
        downloadLabel.setForeground(Color.WHITE);
        versionNumberLabel.setForeground(Color.WHITE);
        fileNameLabel.setForeground(Color.WHITE);

        archiveTypeComboBox.removeAllItems();
        Set<ArchiveType> archiveTypes = new HashSet<>();
        if (featureVersion == 8) {
            pkgsFound8.forEach(pkg -> archiveTypes.add(pkg.getArchiveType()));
        } else if (featureVersion == lastLts.getAsInt()) {
            pkgsFoundLastLts.forEach(pkg -> archiveTypes.add(pkg.getArchiveType()));
        } else if (featureVersion == current.getAsInt()) {
            pkgsFoundCurrent.forEach(pkg -> archiveTypes.add(pkg.getArchiveType()));
        }
        archiveTypes.forEach(archiveType -> archiveTypeComboBox.addItem(archiveType));
        archiveTypeComboBox.setEnabled(archiveTypeComboBox.getItemCount() != 0);

        final PkgInfo selectedPkgInfo = pkgMap.get(featureVersion);
        versionNumberLabel.setText(selectedPkgInfo.getJavaVersion().toString());
        fileNameLabel.setText(selectedPkgInfo.getFileName());
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

    private void updatePkgMap(final Distribution distribution, final Integer... featureVersions) {
        if (null == featureVersions || featureVersions.length == 0) { return; }
        archiveTypeComboBox.removeAllItems();
        archiveTypeComboBox.setEnabled(false);
        pkgsFound8.clear();
        pkgsFoundLastLts.clear();
        pkgsFoundCurrent.clear();
        pkgMap.clear();

        for (Integer featureVersion : featureVersions) {
            pkgs = discoClient.getPkgs(distribution, new VersionNumber(featureVersion), Latest.OVERALL,
                                       discoClient.getOperatingSystem(), Architecture.NONE, Bitness.NONE,
                                       ArchiveType.NONE, PackageType.JDK, false, ReleaseStatus.GA, TermOfSupport.NONE, Scope.PUBLIC);

            if (pkgs.isEmpty()) {
                pkgMap.put(featureVersion, null);
                jdkSelectors.get(featureVersion).setEnabled(false);
                jdkSelectors.get(featureVersion).setToolTipText("Not available for " + discoClient.getOperatingSystem().getUiString());
            } else {
                if (featureVersion == 8) {
                    pkgsFound8 = pkgs.stream().filter(pkg -> pkg.getMajorVersion().getAsInt() == featureVersion).collect(Collectors.toList());
                } else if (featureVersion == lastLts.getAsInt()) {
                    pkgsFoundLastLts = pkgs.stream().filter(pkg -> pkg.getMajorVersion().getAsInt() == featureVersion).collect(Collectors.toList());
                } else if (featureVersion == current.getAsInt()) {
                    pkgsFoundCurrent = pkgs.stream().filter(pkg -> pkg.getMajorVersion().getAsInt() == featureVersion).collect(Collectors.toList());
                }

                Pkg pkgFound = pkgs.stream().filter(pkg -> pkg.getMajorVersion().getAsInt() == featureVersion).findFirst().get();
                pkgMap.put(featureVersion, discoClient.getPkgInfo(pkgFound.getEphemeralId(), pkgFound.getJavaVersion()));
                jdkSelectors.get(featureVersion).setEnabled(true);
                jdkSelectors.get(featureVersion).setToolTipText(null);
            }
        }

        buttonGroup.clearSelection();
        downloadArea.setEnabled(false);
        downloadArea.setBackground(DOWNLOAD_AREA_DISABLED);
        downloadLabel.setForeground(DISABLED_LABEL_COLOR);
        versionNumberLabel.setForeground(DISABLED_LABEL_COLOR);
        fileNameLabel.setForeground(DISABLED_LABEL_COLOR);
        versionNumberLabel.setText("-");
        fileNameLabel.setText("-");
    }

    private void updateSelectedPkg(final int featureVersion, final Pkg pkg) {
        pkgMap.put(featureVersion, discoClient.getPkgInfo(pkg.getEphemeralId(), pkg.getJavaVersion()));
        final PkgInfo selectedPkgInfo = pkgMap.get(featureVersion);
        versionNumberLabel.setText(pkg.getJavaVersion().toString());
        fileNameLabel.setText(selectedPkgInfo.getFileName());
    }

    private Distribution showDistributionDialog(final Container parent) {
        Distribution[] distributions = Distribution.getDistributions().stream().filter(distribution -> Distribution.GRAALVM_CE8 != distribution).filter(distribution -> Distribution.GRAALVM_CE11 != distribution).collect(Collectors.toList()).toArray(new Distribution[0]);
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
