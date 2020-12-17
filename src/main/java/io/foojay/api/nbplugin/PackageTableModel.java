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

import io.foojay.api.discoclient.pkg.ArchiveType;
import io.foojay.api.discoclient.pkg.Distribution;
import io.foojay.api.discoclient.pkg.PackageType;
import io.foojay.api.discoclient.pkg.Pkg;
import io.foojay.api.discoclient.pkg.ReleaseStatus;
import io.foojay.api.discoclient.pkg.SemVer;
import io.foojay.api.discoclient.pkg.TermOfSupport;

import javax.swing.table.AbstractTableModel;
import java.util.List;


public class PackageTableModel extends AbstractTableModel {
    private String[]     columnNames = { "Version", "Distribution", "PackageType", "Release Status", "ArchiveType" };
    private List<Pkg> pkgs;


    public PackageTableModel(final List<Pkg> pkgs) {
        this.pkgs = pkgs;
    }


    public List<Pkg> getPkgs() { return pkgs; }
    public void setPkgs(final List<Pkg> pkgs) {
        this.pkgs = pkgs;
    }

    public String getColumnName(final int col) {
        switch(col) {
            case 0 :
            case 1 :
            case 2 :
            case 3 :
            case 4 : return columnNames[col];
            default: return null;
        }
    }

    public Class getColumnClass(final int col) {
        switch(col) {
            case 0 : return SemVer.class;
            case 1 : return Distribution.class;
            case 2 : return PackageType.class;
            case 3 : return ReleaseStatus.class;
            case 4 : return ArchiveType.class;
            default: return null;
        }
    }

    public Pkg getPkg(final int row) {
        if (row < 0 || row > getRowCount()) { return null; }
        return pkgs.get(row);
    }

    public SemVer getJavaVersion(final int row) {
        if (row < 0 || row > getRowCount()) { return null; }
        return pkgs.get(row).getJavaVersion();
    }

    public Distribution getDistribution(final int row) {
        if (row < 0 || row > getRowCount()) { return null; }
        return pkgs.get(row).getDistribution();
    }

    public PackageType getPkgType(final int row) {
        if (row < 0 || row > getRowCount()) { return null; }
        return pkgs.get(row).getPackageType();
    }

    public TermOfSupport getTermOfSupport(final int row) {
        if (row < 0 || row > getRowCount()) { return null; }
        return pkgs.get(row).getTermOfSupport();
    }

    public ReleaseStatus getReleaseStatus(final int row) {
        if (row < 0 || row > getRowCount()) { return null; }
        return pkgs.get(row).getReleaseStatus();
    }

    public ArchiveType getArchiveType(final int row) {
        if (row < 0 || row > getRowCount()) { return null; }
        return pkgs.get(row).getArchiveType();
    }

    public String getFilename(final int row) {
        if (row < 0 || row > getRowCount()) { return null; }
        return pkgs.get(row).getFileName();
    }

    @Override public int getRowCount() {
        if (null == pkgs) { return 0; }
        return pkgs.size();
    }

    @Override public int getColumnCount() {
        return columnNames.length;
    }

    @Override public Object getValueAt(final int row, final int col) {
        if (row < 0 || row > pkgs.size()) { return null; }
        final Pkg pkg = pkgs.get(row);
        switch(col) {
            case 0 : return pkg.getJavaVersion();
            case 1 : return pkg.getDistribution().getUiString();
            case 2 : return pkg.getPackageType().getUiString();
            case 3 : return pkg.getReleaseStatus().name();
            case 4 : return pkg.getArchiveType().getUiString();
            default: return null;
        }
    }
}
