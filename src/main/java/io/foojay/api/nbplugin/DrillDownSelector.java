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
import io.foojay.api.discoclient.bundle.Architecture;
import io.foojay.api.discoclient.bundle.Bitness;
import io.foojay.api.discoclient.bundle.Bundle;
import io.foojay.api.discoclient.bundle.BundleType;
import io.foojay.api.discoclient.bundle.Distribution;
import io.foojay.api.discoclient.bundle.Extension;
import io.foojay.api.discoclient.bundle.Latest;
import io.foojay.api.discoclient.bundle.Release;
import io.foojay.api.discoclient.bundle.ReleaseStatus;
import io.foojay.api.discoclient.bundle.SupportTerm;
import io.foojay.api.discoclient.bundle.VersionNumber;
import io.foojay.api.discoclient.util.BundleFileInfo;

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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;


public class DrillDownSelector extends JPanel {
    private static final Color                        DOWNLOAD_AREA_STD      = new Color(28, 107, 177);
    private static final Color                        DOWNLOAD_AREA_HOVER    = new Color(4, 124, 192);
    private static final Color                        DOWNLOAD_AREA_DISABLED = new Color(128, 128, 128);
    private static final Color                        DISABLED_LABEL_COLOR   = new Color(190, 190, 190);
    private static final Color                        PROGRESS_BAR_TRACK     = new Color(21, 82, 134);
    private static final Color                        BACKGROUND_COLOR       = new Color(45, 45, 45);
    private static final Color                        TEXT_COLOR             = new Color(164, 164, 164);
    private              DiscoClient                  discoClient;
    private              int                          selectedFeatureVersion;
    private              Release                      jdk8;
    private              Release                      lastLtsRelease;
    private              Release                      currentRelease;
    private              JLabel                       osLabel;
    private              ButtonGroup                  buttonGroup;
    private              Map<Integer, JRadioButton>   jdkSelectors;
    private              JLabel                       distributionLabel;
    private              RJPanel                      downloadArea;
    private              JLabel                       downloadLabel;
    private              JLabel                       versionNumberLabel;
    private              JLabel                       fileNameLabel;
    private              java.util.List<Bundle>       bundles;
    private              JFileChooser                 directoryChooser;
    private              JProgressBar                 progressBar;
    private              JComboBox<Extension>         extensionComboBox;
    private              Map<Integer, BundleFileInfo> bundleMap;
    private              java.util.List<Bundle>       bundlesFound8;
    private              java.util.List<Bundle>       bundlesFoundLastLts;
    private              List<Bundle>                 bundlesFoundCurrent;


    public DrillDownSelector() {
        init();
        registerListeners();

        updateBundleMap(Distribution.ZULU, 8, Integer.valueOf(lastLtsRelease.getVersionNumber()), Integer.valueOf(currentRelease.getVersionNumber()));
    }


    private void init() {
        setPreferredSize(new Dimension(400, 300));

        discoClient            = new DiscoClient();
        bundles                = new ArrayList<>();
        progressBar            = new JProgressBar(0, 100);
        progressBar.setPreferredSize(new Dimension(progressBar.getPreferredSize().width, 5));
        progressBar.setForeground(Color.WHITE);
        progressBar.setValue(0);
        progressBar.setUI(new DrillDownSelector.FlatProgressUI());
        progressBar.setVisible(false);
        extensionComboBox     = new JComboBox<>();
        extensionComboBox.setEnabled(false);
        extensionComboBox.setMaximumSize(new Dimension(80, extensionComboBox.getPreferredSize().height));
        extensionComboBox.addActionListener(e -> {
            if (selectedFeatureVersion == 8) {
                Optional<Bundle> selectedBundle = bundlesFound8.stream().filter(bundle -> bundle.getExtension().equals(extensionComboBox.getSelectedItem())).findFirst();
                if (selectedBundle.isPresent()) { updateSelectedBundle(selectedFeatureVersion, selectedBundle.get());}
            } else if (selectedFeatureVersion == lastLtsRelease.getFeatureVersion()) {
                Optional<Bundle> selectedBundle = bundlesFoundLastLts.stream().filter(bundle -> bundle.getExtension().equals(extensionComboBox.getSelectedItem())).findFirst();
                if (selectedBundle.isPresent()) { updateSelectedBundle(selectedFeatureVersion, selectedBundle.get());}
            } else if (selectedFeatureVersion == currentRelease.getFeatureVersion()) {
                Optional<Bundle> selectedBundle = bundlesFoundCurrent.stream().filter(bundle -> bundle.getExtension().equals(extensionComboBox.getSelectedItem())).findFirst();
                if (selectedBundle.isPresent()) { updateSelectedBundle(selectedFeatureVersion, selectedBundle.get());}
            }
        });
        bundleMap           = new HashMap<>();
        bundlesFound8       = new ArrayList<>();
        bundlesFoundLastLts = new ArrayList<>();
        bundlesFoundCurrent = new ArrayList<>();

        directoryChooser = new JFileChooser();
        directoryChooser.setCurrentDirectory(new File("."));
        directoryChooser.setDialogTitle("Select destination folder");
        directoryChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        directoryChooser.setAcceptAllFileFilterUsed(false);

        osLabel = new JLabel("Download for " + discoClient.getOperatingSystem().getUiString());
        osLabel.setForeground(TEXT_COLOR);

        jdk8           = discoClient.getRelease("8");
        lastLtsRelease = discoClient.getRelease(Release.LAST_LTS_RELEASE);
        currentRelease = discoClient.getRelease(Release.LATEST_RELEASE);

        buttonGroup  = new ButtonGroup();
        jdkSelectors = new ConcurrentHashMap<>();
        jdkSelectors.put(8, createRadioButton(jdk8, buttonGroup));
        jdkSelectors.put(Integer.valueOf(lastLtsRelease.getVersionNumber()), createRadioButton(lastLtsRelease, buttonGroup));
        if (!currentRelease.getVersionNumber().equals(lastLtsRelease.getVersionNumber())) {
            jdkSelectors.put(Integer.valueOf(currentRelease.getVersionNumber()), createRadioButton(currentRelease, buttonGroup));
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

        distributionLabel.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(final MouseEvent e) {
                if (progressBar.isVisible()) { return; }
                Distribution distribution = showDistributionDialog(getParent());
                updateBundleMap(distribution, 8, Integer.valueOf(lastLtsRelease.getVersionNumber()), Integer.valueOf(currentRelease.getVersionNumber()));
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
                    downloadBundle(getParent(), selectedFeatureVersion);
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

    private JRadioButton createRadioButton(final Release release, final ButtonGroup buttonGroup) {
        JRadioButton radioButton = new JRadioButton("JDK " + release.getVersionNumber());
        radioButton.setForeground(TEXT_COLOR);
        radioButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        radioButton.addActionListener(e -> updateDownloadArea(Integer.parseInt(release.getVersionNumber())));
        buttonGroup.add(radioButton);
        return radioButton;
    }

    private void updateDownloadArea(final Integer featureVersion) {
        if (null == featureVersion || !bundleMap.keySet().contains(featureVersion)) { return; }
        selectedFeatureVersion = featureVersion;
        downloadArea.setEnabled(true);
        downloadArea.setBackground(DOWNLOAD_AREA_STD);
        downloadLabel.setForeground(Color.WHITE);
        versionNumberLabel.setForeground(Color.WHITE);
        fileNameLabel.setForeground(Color.WHITE);

        extensionComboBox.removeAllItems();
        if (featureVersion == 8) {
            bundlesFound8.forEach(bundle -> extensionComboBox.addItem(bundle.getExtension()));
        } else if (featureVersion == lastLtsRelease.getFeatureVersion()) {
            bundlesFoundLastLts.forEach(bundle -> extensionComboBox.addItem(bundle.getExtension()));
        } else if (featureVersion == currentRelease.getFeatureVersion()) {
            bundlesFoundCurrent.forEach(bundle -> extensionComboBox.addItem(bundle.getExtension()));
        }
        extensionComboBox.setEnabled(extensionComboBox.getItemCount() != 0);

        final BundleFileInfo selectedBundleInfo = bundleMap.get(featureVersion);
        versionNumberLabel.setText(selectedBundleInfo.getVersionNumber().toString());
        fileNameLabel.setText(selectedBundleInfo.getFileName());
    }

    private void downloadBundle(final Container parent, final Integer featureVersion) {
        if (!downloadArea.isEnabled() || progressBar.isVisible() || null == bundleMap.get(featureVersion)) { return; }

        String targetFolder;
        if (directoryChooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
            targetFolder = directoryChooser.getSelectedFile().getAbsolutePath();
        } else {
            return;
        }

        if (null != targetFolder) {
            BundleFileInfo selectedBundleFileInfo = bundleMap.get(featureVersion);
            long           bundleId               = selectedBundleFileInfo.getId();
            String        fileName      = selectedBundleFileInfo.getFileName();
            VersionNumber versionNumber = selectedBundleFileInfo.getVersionNumber();
            discoClient.downloadBundle(bundleId, targetFolder + File.separator + fileName, versionNumber);
        }
    }

    private void updateBundleMap(final Distribution distribution, final Integer... featureVersions) {
        if (null == featureVersions || featureVersions.length == 0) { return; }
        extensionComboBox.removeAllItems();
        extensionComboBox.setEnabled(false);
        bundlesFound8.clear();
        bundlesFoundLastLts.clear();
        bundlesFoundCurrent.clear();
        bundleMap.clear();

        for (Integer featureVersion : featureVersions) {
            bundles = discoClient.getBundles(distribution, new VersionNumber(featureVersion), Latest.OVERALL,
                                             discoClient.getOperatingSystem(), Architecture.NONE, Bitness.NONE,
                                             Extension.NONE, BundleType.JDK, false, ReleaseStatus.GA, SupportTerm.NONE);

            if (bundles.isEmpty()) {
                bundleMap.put(featureVersion, null);
                jdkSelectors.get(featureVersion).setEnabled(false);
                jdkSelectors.get(featureVersion).setToolTipText("Not available for " + discoClient.getOperatingSystem().getUiString());
            } else {
                if (featureVersion == 8) {
                    bundlesFound8 = bundles.stream().filter(bundle -> bundle.getVersionNumber().getFeature().getAsInt() == featureVersion).collect(Collectors.toList());
                } else if (featureVersion == lastLtsRelease.getFeatureVersion()) {
                    bundlesFoundLastLts = bundles.stream().filter(bundle -> bundle.getVersionNumber().getFeature().getAsInt() == featureVersion).collect(Collectors.toList());
                } else if (featureVersion == currentRelease.getFeatureVersion()) {
                    bundlesFoundCurrent = bundles.stream().filter(bundle -> bundle.getVersionNumber().getFeature().getAsInt() == featureVersion).collect(Collectors.toList());
                }

                Bundle bundleFound = bundles.stream().filter(bundle -> bundle.getVersionNumber().getFeature().getAsInt() == featureVersion).findFirst().get();
                bundleMap.put(featureVersion, discoClient.getBundleFileInfoSCDL(bundleFound.getId(), bundleFound.getVersionNumber()));
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

    private void updateSelectedBundle(final int featureVersion, final Bundle bundle) {
        bundleMap.put(featureVersion, discoClient.getBundleFileInfo(bundle.getId(), bundle.getVersionNumber()));
        final BundleFileInfo selectedBundleInfo = bundleMap.get(featureVersion);
        versionNumberLabel.setText(selectedBundleInfo.getVersionNumber().toString());
        fileNameLabel.setText(selectedBundleInfo.getFileName());
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
