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
import io.foojay.api.discoclient.event.DCEvent;
import io.foojay.api.discoclient.pkg.Architecture;
import io.foojay.api.discoclient.pkg.ArchiveType;
import io.foojay.api.discoclient.pkg.Bitness;
import io.foojay.api.discoclient.pkg.Distribution;
import io.foojay.api.discoclient.pkg.Latest;
import io.foojay.api.discoclient.pkg.MajorVersion;
import io.foojay.api.discoclient.pkg.OperatingSystem;
import io.foojay.api.discoclient.pkg.PackageType;
import io.foojay.api.discoclient.pkg.Pkg;
import io.foojay.api.discoclient.pkg.SemVer;
import io.foojay.api.discoclient.pkg.TermOfSupport;
import io.foojay.api.discoclient.util.PkgInfo;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;


public class Main {
    private static final int        PREFERRED_WIDTH  = 600;
    private static final int        PREFERRED_HEIGHT = 300;
    private DiscoClient             discoClient;
    private JComboBox<SemVer>       versionComboBox;
    private JComboBox<Distribution> distributionComboBox;
    private JComboBox<PackageType>  packageTypeComboBox;
    private JComboBox<ArchiveType>  archiveType;
    private PackageTableModel       tableModel;
    private JTable                  table;
    private JLabel                  filenameLabel;
    private JProgressBar            progressBar;
    private JButton                 downloadButton;


    public Main() {
        JFrame frame = new JFrame("Foojay Disco API");
        frame.setSize(PREFERRED_WIDTH, PREFERRED_HEIGHT);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);


        // Setup disco client
        discoClient = new DiscoClient();
        discoClient.setOnDCEvent(e -> handleDCEvent(frame, e));


        // Get release infos
        MajorVersion lastLtsRelease        = discoClient.getLatestLts(false);
        Integer      lastLtsFeatureRelease = lastLtsRelease.getAsInt();

        // Versions
        List<MajorVersion> allMajorVersions = discoClient.getAllMajorVersions(Optional.empty(), Optional.of(Boolean.TRUE), Optional.empty());
        HashSet<SemVer>    allVersions      = new HashSet<>();
        allMajorVersions.forEach(majorVersion -> allVersions.addAll(majorVersion.getVersions()));
        List<SemVer> sortedVersions = allVersions.stream().sorted(Comparator.comparing(SemVer::getVersionNumber).reversed()).collect(Collectors.toList());

        JLabel versionLabel = new JLabel("Versions");
        versionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        //List<Integer> versionNumbers = new ArrayList<>();
        //for (Integer i = 6 ; i <= nextFeatureRelease ; i++) { versionNumbers.add(i); }
        versionComboBox = new JComboBox<>(sortedVersions.toArray(new SemVer[0]));
        versionComboBox.setSelectedItem(lastLtsFeatureRelease);
        versionComboBox.addActionListener(e -> updateData());

        Box versionsVBox = Box.createVerticalBox();
        versionsVBox.add(versionLabel);
        versionsVBox.add(versionComboBox);
        versionsVBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));


        // Distributions
        JLabel distributionLabel = new JLabel("Distributions");
        distributionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        Distribution[] distributions = { Distribution.AOJ, Distribution.AOJ_OPENJ9, Distribution.CORRETTO, Distribution.DRAGONWELL, Distribution.LIBERICA, Distribution.ORACLE, Distribution.ORACLE_OPEN_JDK, Distribution.RED_HAT, Distribution.SAP_MACHINE, Distribution.ZULU };
        distributionComboBox = new JComboBox<>(distributions);
        distributionComboBox.setRenderer(new DistributionListCellRenderer());
        distributionComboBox.setSelectedItem(Distribution.ZULU);
        distributionComboBox.addActionListener(e -> updateData());

        Box distributionVBox = Box.createVerticalBox();
        distributionVBox.add(distributionLabel);
        distributionVBox.add(distributionComboBox);
        distributionVBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));


        // Bundle Types
        JLabel packageTypeLabel = new JLabel("Bundle Type");
        packageTypeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        PackageType[] packageTypes = Arrays.stream(PackageType.values()).filter(packageType -> PackageType.NONE != packageType).filter(packageType -> PackageType.NOT_FOUND != packageType).toArray(PackageType[]::new);
        packageTypeComboBox = new JComboBox<>(packageTypes);
        packageTypeComboBox.addActionListener(e -> updateData());

        Box packageTypeVBox = Box.createVerticalBox();
        packageTypeVBox.add(packageTypeLabel);
        packageTypeVBox.add(packageTypeComboBox);
        packageTypeVBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));


        // ArchiveType
        List<ArchiveType> availableArchiveTypes = new ArrayList<>(discoClient.getArchiveTypes(discoClient.getOperatingSystem()));
        availableArchiveTypes.add(0, ArchiveType.NONE);
        JLabel extensionLabel = new JLabel("ArchiveType");
        extensionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        ArchiveType[] extensions = availableArchiveTypes.toArray(ArchiveType[]::new);
        archiveType = new JComboBox(extensions);
        archiveType.setRenderer(new ArchiveTypeListCellRenderer());
        archiveType.addActionListener(e -> updateData());

        Box extensionVBox = Box.createVerticalBox();
        extensionVBox.add(extensionLabel);
        extensionVBox.add(archiveType);
        extensionVBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));


        // Header Box
        Box hBox = Box.createHorizontalBox();
        hBox.add(versionsVBox);
        hBox.add(distributionVBox);
        hBox.add(packageTypeVBox);
        hBox.add(extensionVBox);

        Box vBox = Box.createVerticalBox();
        vBox.add(hBox);

        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.PAGE_AXIS));
        headerPanel.add(hBox);


        // Table
        Font tableFont = versionLabel.getFont();
        tableFont  = new Font(tableFont.getName(), Font.PLAIN, 13);
        tableModel = new PackageTableModel(List.of());
        table      = new JTable(tableModel);
        table.setFont(tableFont);
        table.setOpaque(true);
        table.setFillsViewportHeight(true);
        table.setBackground(new Color(45, 45, 45));
        table.setForeground(new Color(164, 164, 164));
        table.getTableHeader().setBackground(new Color(45, 45, 45));
        table.getTableHeader().setForeground(new Color(164, 164, 164));
        table.getTableHeader().setFont(tableFont);
        table.setSelectionBackground(new Color(4, 124, 192));
        table.setSelectionForeground(Color.WHITE);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);
        ListSelectionModel selectionModel = table.getSelectionModel();
        selectionModel.addListSelectionListener(e -> {
            downloadButton.setEnabled(table.getSelectedRow() >= 0);
            filenameLabel.setText(tableModel.getFilename(table.getSelectedRow()));
        });
        JScrollPane tableScrollPane = new JScrollPane(table);
        tableScrollPane.setPreferredSize(new Dimension(PREFERRED_WIDTH, PREFERRED_HEIGHT));


        // Footer Box
        filenameLabel = new JLabel("-");

        progressBar = new JProgressBar(0, 100);
        progressBar.setValue(0);
        progressBar.setStringPainted(true);

        downloadButton = new JButton("Download");
        downloadButton.setEnabled(false);
        downloadButton.addActionListener(e -> downloadBundle(frame));

        Box footerHBox = Box.createHorizontalBox();
        footerHBox.add(progressBar);
        footerHBox.add(downloadButton);

        Box footerVBox = Box.createVerticalBox();
        footerVBox.add(filenameLabel);
        footerVBox.add(footerHBox);

        JPanel footerPanel = new JPanel();
        footerPanel.setBorder(new EmptyBorder(5, 10, 5, 10));
        footerPanel.setLayout(new BoxLayout(footerPanel, BoxLayout.PAGE_AXIS));
        footerPanel.add(footerVBox);


        // Setup main layout
        frame.getContentPane().add(headerPanel, BorderLayout.NORTH);
        frame.getContentPane().add(tableScrollPane, BorderLayout.CENTER);
        frame.getContentPane().add(footerPanel, BorderLayout.SOUTH);


        // Show frame
        frame.pack();
        frame.setVisible(true);

        updateData();
    }

    private void updateData() {
        Distribution    distribution    = (Distribution) distributionComboBox.getSelectedItem();
        SemVer          semVer          = (SemVer) versionComboBox.getSelectedItem();
        OperatingSystem operatingSystem = discoClient.getOperatingSystem();
        Architecture    architecture    = Architecture.NONE;
        Bitness         bitness         = Bitness.NONE;
        ArchiveType     archiveType     = (ArchiveType) this.archiveType.getSelectedItem();
        PackageType     packageType     = (PackageType) packageTypeComboBox.getSelectedItem();
        Boolean         javaFxBundled   = false;
        TermOfSupport   termOfSupport   = TermOfSupport.NONE;

        List<Pkg>       packages        = new ArrayList<>();
        packages.addAll(discoClient.getPkgs(distribution, semVer.getVersionNumber(), Latest.OVERALL, operatingSystem, architecture, bitness, archiveType, packageType, javaFxBundled, semVer.getReleaseStatus(), termOfSupport));
        //packages.addAll(discoClient.getPkgs(distribution, semVer.getVersionNumber(), Latest.OVERALL, operatingSystem, architecture, bitness, archiveType, packageType, javaFxBundled, semVer.getReleaseStatus(), termOfSupport));
        List<Pkg>       sortedPackages  = packages.stream()
                                                  .sorted(Comparator.comparing(Pkg::getDistributionName)
                                                  .thenComparing(Pkg::getJavaVersion).reversed()
                                                  .thenComparing(Pkg::getOperatingSystem)
                                                  .thenComparing(Pkg::getArchitecture))
                                                  .collect(Collectors.toList());

        SwingUtilities.invokeLater(() -> {
            PackageTableModel tableModel = (PackageTableModel) table.getModel();
            tableModel.setPkgs(sortedPackages);
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

        Pkg       pkg           = tableModel.getPkgs().get(table.getSelectedRow());
        SemVer    versionNumber = pkg.getJavaVersion();
        PkgInfo   pkgInfo       = discoClient.getPkgInfo(pkg.getId(), versionNumber);
        String    fileName      = destinationFolder+  File.separator + pkgInfo.getFileName();
        Future<?> future        = discoClient.downloadPkg(pkgInfo, fileName);
        try {
            assert null == future.get();
        } catch (InterruptedException | ExecutionException e) {

        }
    }

    public static void main(String[] args) {
        new Main();
    }
}
