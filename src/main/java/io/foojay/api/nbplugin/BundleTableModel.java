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

import io.foojay.api.discoclient.bundle.Bundle;
import io.foojay.api.discoclient.bundle.BundleType;
import io.foojay.api.discoclient.bundle.Distribution;
import io.foojay.api.discoclient.bundle.Extension;
import io.foojay.api.discoclient.bundle.ReleaseStatus;
import io.foojay.api.discoclient.bundle.SupportTerm;
import io.foojay.api.discoclient.bundle.VersionNumber;

import javax.swing.table.AbstractTableModel;
import java.util.List;


public class BundleTableModel extends AbstractTableModel {
    private String[]     columnNames = { "Version", "Distribution", "Vendor", "Bundle Type", "Release Status", "Extension" };
    private List<Bundle> bundles;


    public BundleTableModel(final List<Bundle> bundles) {
        this.bundles = bundles;
    }


    public List<Bundle> getBundles() { return bundles; }
    public void setBundles(final List<Bundle> bundles) {
        this.bundles = bundles;
    }

    public String getColumnName(final int col) {
        switch(col) {
            case 0 :
            case 1 :
            case 2 :
            case 3 :
            case 4 :
            case 5 : return columnNames[col];
            default: return null;
        }
    }

    public Class getColumnClass(final int col) {
        switch(col) {
            case 0 : return VersionNumber.class;
            case 1 : return Distribution.class;
            case 2 : return String.class;
            case 3 : return BundleType.class;
            case 4 : return ReleaseStatus.class;
            case 5 : return Extension.class;
            default: return null;
        }
    }

    public Bundle getBundle(final int row) {
        if (row < 0 || row > getRowCount()) { return null; }
        return bundles.get(row);
    }

    public VersionNumber getVersionNumber(final int row) {
        if (row < 0 || row > getRowCount()) { return null; }
        return bundles.get(row).getVersionNumber();
    }

    public Distribution getDistribution(final int row) {
        if (row < 0 || row > getRowCount()) { return null; }
        return bundles.get(row).getDistribution();
    }

    public String getVendor(final int row) {
        if (row < 0 || row > getRowCount()) { return null; }
        return bundles.get(row).getDistribution().getVendor();
    }

    public BundleType getBundleType(final int row) {
        if (row < 0 || row > getRowCount()) { return null; }
        return bundles.get(row).getBundleType();
    }

    public SupportTerm getSupportTerm(final int row) {
        if (row < 0 || row > getRowCount()) { return null; }
        return bundles.get(row).getSupportTerm();
    }

    public ReleaseStatus getReleaseStatus(final int row) {
        if (row < 0 || row > getRowCount()) { return null; }
        return bundles.get(row).getReleaseStatus();
    }

    public Extension getExtension(final int row) {
        if (row < 0 || row > getRowCount()) { return null; }
        return bundles.get(row).getExtension();
    }

    public String getFilename(final int row) {
        if (row < 0 || row > getRowCount()) { return null; }
        return bundles.get(row).getFileName();
    }

    @Override public int getRowCount() {
        if (null == bundles) { return 0; }
        return bundles.size();
    }

    @Override public int getColumnCount() {
        return columnNames.length;
    }

    @Override public Object getValueAt(final int row, final int col) {
        if (row < 0 || row > bundles.size()) { return null; }
        final Bundle bundle = bundles.get(row);
        switch(col) {
            case 0 : return bundle.getVersionNumber();
            case 1 : return bundle.getDistribution().getUiString();
            case 2 : return bundle.getDistribution().getVendor();
            case 3 : return bundle.getBundleType().getUiString();
            case 4 : return bundle.getReleaseStatus().name();
            case 5 : return bundle.getExtension().getUiString();
            default: return null;
        }
    }
}
