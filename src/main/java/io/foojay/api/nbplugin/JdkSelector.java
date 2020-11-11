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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class JdkSelector extends JPanel {
    private static final Color         DOWNLOAD_AREA_STD      = new Color(28, 107, 177);
    private static final Color         DOWNLOAD_AREA_HOVER    = new Color(4, 124, 192);
    private static final Color         DOWNLOAD_AREA_DISABLED = new Color(128, 128, 128);
    private static final Color         DISABLED_LABEL_COLOR   = new Color(190, 190, 190);
    private static final Color         PROGRESS_BAR_TRACK     = new Color(21, 82, 134);
    private DiscoClient                discoClient;
    private Distribution               distribution;
    private JLabel                     osLabel;
    private ButtonGroup                buttonGroup;
    private Map<Integer, JRadioButton> jdkSelectors;
    private JLabel                     distributionLabel;
    private RJPanel                    downloadArea;
    private JLabel                     downloadLabel;
    private JLabel                     versionNumberLabel;
    private JLabel                     fileNameLabel;
    private List<Bundle>               bundles;
    private Bundle                     selectedBundle;
    private BundleFileInfo             selectedBundleFileInfo;
    private JFileChooser               directoryChooser;
    private JProgressBar               progressBar;


    public JdkSelector() {
        setPreferredSize(new Dimension(400, 300));

        discoClient            = new DiscoClient();
        distribution           = Distribution.ZULU;
        bundles                = new ArrayList<>();
        selectedBundle         = null;
        selectedBundleFileInfo = null;
        progressBar            = new JProgressBar(0, 100);
        progressBar.setPreferredSize(new Dimension(progressBar.getPreferredSize().width, 5));
        progressBar.setForeground(Color.WHITE);
        progressBar.setValue(0);
        progressBar.setUI(new FlatProgressUI());
        progressBar.setVisible(false);


        directoryChooser = new JFileChooser();
        directoryChooser.setCurrentDirectory(new File("."));
        directoryChooser.setDialogTitle("Select destination folder");
        directoryChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        directoryChooser.setAcceptAllFileFilterUsed(false);

        osLabel = new JLabel("Download for " + discoClient.getOperatingSystem().getUiString());

        Release jdk8           = discoClient.getRelease("8");
        Release lastLtsRelease = discoClient.getRelease(Release.LAST_LTS_RELEASE);
        Release currentRelease = discoClient.getRelease(Release.LATEST_RELEASE);

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

        registerListeners();
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
                distribution = showDistributionDialog(getParent());
                buttonGroup.clearSelection();
                selectedBundle         = null;
                selectedBundleFileInfo = null;
                downloadLabel.setForeground(DISABLED_LABEL_COLOR);
                versionNumberLabel.setText("-");
                versionNumberLabel.setForeground(DISABLED_LABEL_COLOR);
                fileNameLabel.setText("-");
                fileNameLabel.setForeground(DISABLED_LABEL_COLOR);
                downloadArea.setBackground(DOWNLOAD_AREA_DISABLED);
                downloadArea.setEnabled(false);
                jdkSelectors.entrySet().forEach(entry -> entry.getValue().setEnabled(true));
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
                    downloadBundle(getParent());
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
        radioButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        radioButton.addActionListener(e -> update(Integer.parseInt(release.getVersionNumber())));
        buttonGroup.add(radioButton);
        return radioButton;
    }

    private void update(final int featureVersion) {
        bundles = discoClient.getBundles(distribution,
                                         new VersionNumber(featureVersion),
                                         Latest.OVERALL,
                                         discoClient.getOperatingSystem(),
                                         Architecture.NONE,
                                         Bitness.NONE,
                                         Extension.NONE,
                                         BundleType.JDK,
                                         false,
                                         ReleaseStatus.NONE,
                                         SupportTerm.NONE);

        if (bundles.isEmpty()) {
            downloadArea.setEnabled(false);
            downloadArea.setBackground(DOWNLOAD_AREA_DISABLED);
            downloadLabel.setForeground(DISABLED_LABEL_COLOR);
            versionNumberLabel.setForeground(DISABLED_LABEL_COLOR);
            fileNameLabel.setForeground(DISABLED_LABEL_COLOR);

            jdkSelectors.get(featureVersion).setSelected(false);
            jdkSelectors.get(featureVersion).setEnabled(false);

            selectedBundle         = null;
            selectedBundleFileInfo = null;

            versionNumberLabel.setText("-");
            fileNameLabel.setText("-");
        } else {
            downloadArea.setEnabled(true);
            downloadArea.setBackground(DOWNLOAD_AREA_STD);
            downloadLabel.setForeground(Color.WHITE);
            versionNumberLabel.setForeground(Color.WHITE);
            fileNameLabel.setForeground(Color.WHITE);

            selectedBundle         = bundles.stream().filter(bundle -> bundle.getVersionNumber().getFeature().getAsInt() == featureVersion).findFirst().get();
            selectedBundleFileInfo = discoClient.getBundleFileInfo(selectedBundle.getId());

            versionNumberLabel.setText(selectedBundle.getVersionNumber().toString());
            fileNameLabel.setText(selectedBundle.getFileName());
        }
    }

    private void downloadBundle(final Container parent) {
        if (!downloadArea.isEnabled() || progressBar.isVisible()) { return; }

        String targetFolder;
        if (directoryChooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
            targetFolder = directoryChooser.getSelectedFile().getAbsolutePath();
        } else {
            return;
        }

        if (null != targetFolder) {
            long   bundleId = selectedBundleFileInfo.getId();
            String fileName = selectedBundleFileInfo.getFileName();
            discoClient.downloadBundle(bundleId, targetFolder + File.separator + fileName);
        }
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
