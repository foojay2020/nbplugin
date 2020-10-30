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
import io.foojay.api.discoclient.bundle.OperatingSystem;
import io.foojay.api.discoclient.bundle.Release;
import io.foojay.api.discoclient.bundle.ReleaseStatus;
import io.foojay.api.discoclient.bundle.SupportTerm;
import io.foojay.api.discoclient.bundle.VersionNumber;
import io.foojay.api.discoclient.event.DCEvent;
import io.foojay.api.discoclient.util.BundleFileInfo;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;


public class Main {
    private DiscoClient             discoClient;
    private JComboBox<Integer>      versionComboBox;
    private JComboBox<Distribution> distributionComboBox;
    private JComboBox<BundleType>   bundleTypeComboBox;
    private JCheckBox               latestCheckBox;
    private BundleTableModel        tableModel;
    private JTable                  table;
    private JProgressBar            progressBar;
    private JButton                 downloadButton;


    public Main() {
        JFrame frame = new JFrame("Foojay Disco API");
        frame.setSize(280, 100);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);


        // Setup disco client
        discoClient = new DiscoClient();
        discoClient.setOnDCEvent(e -> handleDCEvent(frame, e));


        // Get release infos
        Release lastLtsRelease = discoClient.getRelease(Release.LAST_LTS_RELEASE);
        Integer lastLtsFeatureRelease = Integer.valueOf(lastLtsRelease.getVersionNumber());

        Release nextRelease = discoClient.getRelease(Release.NEXT_RELEASE);
        Integer nextFeatureRelease = Integer.valueOf(nextRelease.getVersionNumber());


        // Versions
        JLabel versionLabel = new JLabel("Versions");
        versionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        List<Integer> versionNumbers = new ArrayList<>();
        for (Integer i = 6 ; i <= nextFeatureRelease ; i++) { versionNumbers.add(i); }
        versionComboBox = new JComboBox<>(versionNumbers.toArray(new Integer[0]));
        versionComboBox.setSelectedItem(lastLtsFeatureRelease);
        versionComboBox.addActionListener(e -> updateData());

        Box versionsVBox = Box.createVerticalBox();
        versionsVBox.add(versionLabel);
        versionsVBox.add(versionComboBox);
        versionsVBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));


        // Distributions
        JLabel distributionLabel = new JLabel("Distributions");
        distributionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        Distribution[] distributions = { Distribution.NONE, Distribution.ADOPT, Distribution.CORRETTO, Distribution.DRAGONWELL, Distribution.LIBERICA, Distribution.OPEN_JDK, Distribution.SAP_MACHINE, Distribution.ZULU };
        distributionComboBox = new JComboBox<>(distributions);
        distributionComboBox.setRenderer(new DistributionListCellRenderer());
        distributionComboBox.addActionListener(e -> updateData());

        Box distributionVBox = Box.createVerticalBox();
        distributionVBox.add(distributionLabel);
        distributionVBox.add(distributionComboBox);
        distributionVBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));


        // Bundle Types
        JLabel bundleTypeLabel = new JLabel("Bundle Type");
        bundleTypeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        BundleType[] bundleTypes = Arrays.stream(BundleType.values()).filter(bundleType -> BundleType.NONE != bundleType).filter(bundleType -> BundleType.NOT_FOUND != bundleType).toArray(BundleType[]::new);
        bundleTypeComboBox = new JComboBox<>(bundleTypes);
        bundleTypeComboBox.addActionListener(e -> updateData());

        Box bundleTypeVBox = Box.createVerticalBox();
        bundleTypeVBox.add(bundleTypeLabel);
        bundleTypeVBox.add(bundleTypeComboBox);
        bundleTypeVBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));


        // Latest
        JLabel latestLabel = new JLabel("Latest");
        latestLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        latestCheckBox = new JCheckBox();
        latestCheckBox.addActionListener(e -> updateData());

        Box latestVBox = Box.createVerticalBox();
        latestVBox.add(latestLabel);
        latestVBox.add(latestCheckBox);
        latestVBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));


        // Header Box
        Box hBox = Box.createHorizontalBox();
        hBox.add(distributionVBox);
        hBox.add(versionsVBox);
        hBox.add(bundleTypeVBox);
        hBox.add(latestVBox);

        Box vBox = Box.createVerticalBox();
        vBox.add(hBox);

        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.PAGE_AXIS));
        headerPanel.add(hBox);


        // Table
        tableModel = new BundleTableModel(List.of());
        table      = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);
        ListSelectionModel selectionModel = table.getSelectionModel();
        selectionModel.addListSelectionListener(e -> downloadButton.setEnabled(table.getSelectedRow() >= 0));
        JScrollPane tableScrollPane = new JScrollPane(table);
        tableScrollPane.setPreferredSize(new Dimension(400, 300));


        // Footer Box
        progressBar = new JProgressBar(0, 100);
        progressBar.setValue(0);
        progressBar.setStringPainted(true);

        downloadButton = new JButton("Download");
        downloadButton.setEnabled(false);
        downloadButton.addActionListener(e -> downloadBundle(frame));

        Box fBox = Box.createHorizontalBox();
        fBox.add(progressBar);
        fBox.add(downloadButton);

        JPanel footerPanel = new JPanel();
        footerPanel.setLayout(new BoxLayout(footerPanel, BoxLayout.PAGE_AXIS));
        footerPanel.add(fBox);


        // Setup main layout
        frame.getContentPane().add(headerPanel, BorderLayout.NORTH);
        frame.getContentPane().add(tableScrollPane, BorderLayout.CENTER);
        frame.getContentPane().add(footerPanel, BorderLayout.SOUTH);


        // Show frame
        frame.pack();
        frame.setVisible(true);
    }

    private void updateData() {
        Distribution    distribution    = (Distribution) distributionComboBox.getSelectedItem();
        Integer         featureVersion  = (Integer) versionComboBox.getSelectedItem();
        boolean         latest          = latestCheckBox.isSelected();
        OperatingSystem operatingSystem = getOperatingSystem();
        Architecture    architecture    = Architecture.NONE;
        Bitness         bitness         = Bitness.NONE;
        Extension       extension       = Extension.NONE;
        BundleType      bundleType      = (BundleType) bundleTypeComboBox.getSelectedItem();
        Boolean         fx              = false;
        ReleaseStatus   releaseStatus   = ReleaseStatus.NONE;
        SupportTerm     supportTerm     = SupportTerm.NONE;
        List<Bundle>    bundles         = discoClient.getBundles(distribution, new VersionNumber(featureVersion), latest, operatingSystem, architecture, bitness, extension, bundleType, fx, releaseStatus,  supportTerm);
        SwingUtilities.invokeLater(() -> {
            BundleTableModel tableModel = (BundleTableModel) table.getModel();
            tableModel.setBundles(bundles);
            tableModel.fireTableDataChanged();
        });
    }

    private void handleDCEvent(final Component parent, final DCEvent event) {
        switch(event.getType()) {
            case DOWNLOAD_STARTED :
                SwingUtilities.invokeLater(() -> downloadButton.setEnabled(false));
                break;
            case DOWNLOAD_FINISHED:
                SwingUtilities.invokeLater(() -> {
                    progressBar.setValue(0);
                    downloadButton.setEnabled(true);
                });
                break;
            case DOWNLOAD_PROGRESS:
                SwingUtilities.invokeLater(() -> progressBar.setValue((int) ((double) event.getFraction() / (double) event.getFileSize() * 100)));
                break;
            case DOWNLOAD_FAILED:
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(parent, "Download failed", "Attention", JOptionPane.WARNING_MESSAGE));
                break;
        }
    }

    private void downloadBundle(final Component parent) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setCurrentDirectory(new File("."));
        fileChooser.setDialogTitle("Select destination folder");
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setAcceptAllFileFilterUsed(false);

        String destinationFolder;
        if (fileChooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
            destinationFolder = fileChooser.getSelectedFile().getAbsolutePath();
        } else {
            return;
        }

        Bundle         bundle         = tableModel.getBundles().get(table.getSelectedRow());
        BundleFileInfo bundleFileInfo = discoClient.getBundleFileInfo(bundle.getId());
        String         fileName       = destinationFolder+  File.separator + bundleFileInfo.getFileName();

        Future<?> future = discoClient.downloadBundle(bundle.getId(), fileName);
        try {
            assert null == future.get();
        } catch (InterruptedException | ExecutionException e) {

        }
    }

    private OperatingSystem getOperatingSystem() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.indexOf("win") >= 0) {
            return OperatingSystem.WINDOWS;
        } else if (os.indexOf("mac") >= 0) {
            return OperatingSystem.MACOS;
        } else if (os.indexOf("nix") >= 0 || os.indexOf("nux") >= 0) {
            return OperatingSystem.LINUX;
        } else if (os.indexOf("sunos") >= 0) {
            return OperatingSystem.SOLARIS;
        } else {
            return OperatingSystem.NONE;
        }
    }

    public static void main(String[] args) {
        new Main();
    }
}