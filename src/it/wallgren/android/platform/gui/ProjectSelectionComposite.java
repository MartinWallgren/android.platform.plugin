/**
 * Copyright 2012 Martin Wallgren
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

package it.wallgren.android.platform.gui;

import java.util.LinkedList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

public class ProjectSelectionComposite extends Composite {
    private final Table table;
    private final List<CompositeListener> compositeListeners;

    /**
     * Create the composite.
     *
     * @param parent
     * @param style
     */
    public ProjectSelectionComposite(Composite parent, int style) {
        super(parent, style);
        compositeListeners = new LinkedList<CompositeListener>();
        setLayout(new FormLayout());

        final Button btnNone = new Button(this, SWT.BORDER | SWT.FLAT);
        final FormData fd_btnNone = new FormData();
        fd_btnNone.top = new FormAttachment(0, 10);
        fd_btnNone.right = new FormAttachment(100, -10);
        btnNone.setLayoutData(fd_btnNone);

        btnNone.setText("Select None");
        btnNone.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                setCheckedForAll(false);
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
            }
        });

        final Button btnAll = new Button(this, SWT.NONE);
        final FormData fd_btnAll = new FormData();
        fd_btnAll.right = new FormAttachment(btnNone, -6);
        fd_btnAll.top = new FormAttachment(0, 10);
        btnAll.setLayoutData(fd_btnAll);

        btnAll.setText("Select All");
        btnAll.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                setCheckedForAll(true);
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
            }
        });
        table = new Table(this, SWT.BORDER | SWT.CHECK | SWT.FULL_SELECTION | SWT.HIDE_SELECTION);
        final FormData fd_table = new FormData();
        fd_table.bottom = new FormAttachment(100, -10);
        fd_table.right = new FormAttachment(100, -10);
        fd_table.top = new FormAttachment(btnNone, 10);
        fd_table.left = new FormAttachment(0, 10);
        table.setLayoutData(fd_table);
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        table.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                notifyListeners();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                notifyListeners();
            }
        });
    }

    protected void setCheckedForAll(boolean checked) {
        final TableItem[] items = table.getItems();
        for (final TableItem tableItem : items) {
            tableItem.setChecked(checked);
        }
        notifyListeners();
    }

    public TableItem[] getItems() {
        return table.getItems();
    }

    public TableItem createTableItem() {
        return new TableItem(table, SWT.NONE);
    }

    public void addListener(CompositeListener listener) {
        compositeListeners.add(listener);
    }

    public void removeListener(CompositeListener listener) {
        compositeListeners.remove(listener);
    }

    private void notifyListeners() {
        for (final CompositeListener listener : compositeListeners) {
            listener.onCompositeChanged();
        }
    }
}
