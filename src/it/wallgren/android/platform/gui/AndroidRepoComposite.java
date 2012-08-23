
package it.wallgren.android.platform.gui;

import java.util.LinkedList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class AndroidRepoComposite extends Composite {
    private Text text;
    private List<CompositeListener> compositeListeners;

    /**
     * Create the composite.
     * 
     * @param parent
     * @param style
     */
    public AndroidRepoComposite(Composite parent, int style) {
        super(parent, style);
        compositeListeners = new LinkedList<CompositeListener>();
        setLayout(new GridLayout(3, false));

        Label lblAndroidRepo = new Label(this, SWT.NONE);
        lblAndroidRepo.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
        lblAndroidRepo.setText("Android Repo");

        text = new Text(this, SWT.BORDER);
        text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        text.setMessage("/enter/path/to/android/repository");
        text.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                String path = text.getText();
                if (path.contains("~")) {
                    text.removeModifyListener(this);
                    text.setText("");
                    text.append(path.replace("~", System.getProperty("user.home")));
                    text.addModifyListener(this);
                }
                notifyListeners();
            }
        });

        Button btnBrowse = new Button(this, SWT.NONE);
        btnBrowse.setText("Browse");
        btnBrowse.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                DirectoryDialog dialog = new DirectoryDialog(getShell(), SWT.NULL);
                String path = dialog.open();
                if (path != null) {
                    text.setText(path);
                }
                notifyListeners();
            }
        });
    }

    public String getRepoPath() {
        return text.getText();
    }

    public void addListener(CompositeListener listener) {
        compositeListeners.add(listener);
    }

    public void removeListener(CompositeListener listener) {
        compositeListeners.remove(listener);
    }

    private void notifyListeners() {
        for (CompositeListener listener : compositeListeners) {
            listener.onCompositeChanged();
        }
    }
}
