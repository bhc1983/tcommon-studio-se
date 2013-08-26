// ============================================================================
//
// Copyright (C) 2006-2013 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.repository.items.importexport.ui.wizard.imports;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.internal.IWorkbenchGraphicConstants;
import org.eclipse.ui.internal.WorkbenchImages;
import org.eclipse.ui.internal.ide.IDEWorkbenchPlugin;
import org.eclipse.ui.internal.wizards.datatransfer.ArchiveFileManipulations;
import org.eclipse.ui.internal.wizards.datatransfer.TarException;
import org.eclipse.ui.internal.wizards.datatransfer.TarFile;
import org.eclipse.ui.internal.wizards.datatransfer.TarLeveledStructureProvider;
import org.eclipse.ui.internal.wizards.datatransfer.ZipLeveledStructureProvider;
import org.talend.commons.exception.ExceptionHandler;
import org.talend.commons.exception.LoginException;
import org.talend.commons.exception.PersistenceException;
import org.talend.core.GlobalServiceRegister;
import org.talend.core.PluginChecker;
import org.talend.core.model.properties.Item;
import org.talend.core.model.repository.RepositoryViewObject;
import org.talend.core.model.utils.TalendPropertiesUtil;
import org.talend.core.repository.model.ProxyRepositoryFactory;
import org.talend.core.service.IExchangeService;
import org.talend.core.ui.advanced.composite.FilteredCheckboxTree;
import org.talend.repository.items.importexport.handlers.ImportExportHandlersManager;
import org.talend.repository.items.importexport.i18n.Messages;
import org.talend.repository.items.importexport.ui.wizard.imports.managers.ImportItemUtil;
import org.talend.repository.items.importexport.ui.wizard.imports.managers.ProviderManager;
import org.talend.repository.items.importexport.ui.wizard.imports.managers.ResourcesManager;
import org.talend.repository.items.importexport.ui.wizard.imports.managers.ResourcesManagerFactory;
import org.talend.repository.items.importexport.ui.wizard.imports.models.ImportNodesBuilder;
import org.talend.repository.items.importexport.ui.wizard.imports.models.ItemImportNode;
import org.talend.repository.items.importexport.ui.wizard.imports.models.ItemRecord;
import org.talend.repository.items.importexport.ui.wizard.imports.providers.ImportItemsViewerContentProvider;
import org.talend.repository.items.importexport.ui.wizard.imports.providers.ImportItemsViewerFilter;
import org.talend.repository.items.importexport.ui.wizard.imports.providers.ImportItemsViewerLabelProvider;
import org.talend.repository.items.importexport.ui.wizard.imports.providers.ImportItemsViewerSorter;
import org.talend.repository.model.ERepositoryStatus;
import org.talend.repository.model.IProxyRepositoryFactory;

/**
 * 
 * DOC ggu class global comment. Detailled comment
 */
public class ImportItemsWizardPage extends WizardPage {

    private Button itemFromDirectoryRadio, itemFromArchiveRadio;

    private Text directoryPathField, archivePathField;

    private Button browseDirectoriesButton, browseArchivesButton, fromExchangeButton;

    private FilteredCheckboxTree filteredCheckboxTree;

    private TableViewer errorsListViewer;

    private final List<String> errors = new ArrayList<String>();

    private Button overwriteButton;

    /*
     * 
     */
    private static final String[] ARCHIVE_FILE_MASK = { "*.jar;*.zip;*.tar;*.tar.gz;*.tgz", "*.*" }; //$NON-NLS-1$ //$NON-NLS-2$

    private String previouslyBrowsedDirectoryPath, previouslyBrowsedArchivePath, lastWorkedPath;

    private List<ItemRecord> selectedItemRecords = new ArrayList<ItemRecord>();

    private final ImportNodesBuilder nodesBuilder = new ImportNodesBuilder();

    private ResourcesManager resManager;

    /**
     * 
     * DOC ggu ImportItemsWizardPage constructor comment.
     * 
     * @param pageName
     */
    @SuppressWarnings("restriction")
    public ImportItemsWizardPage(String pageName) {

        super(pageName);
        setDescription(Messages.getString("ImportItemsWizardPage_importDescription")); //$NON-NLS-1$
        setImageDescriptor(WorkbenchImages.getImageDescriptor(IWorkbenchGraphicConstants.IMG_WIZBAN_IMPORT_WIZ));
    }

    @Override
    public void createControl(Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        setControl(composite);
        composite.setLayout(new GridLayout());
        composite.setLayoutData(new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL));

        createSelectionArea(composite);
        createItemListArea(composite);
        createErrorsListArea(composite);
        createAdditionArea(composite);

        Dialog.applyDialogFont(composite);
    }

    private void createSelectionArea(Composite parent) {
        Composite selectionArea = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.numColumns = 4;
        layout.makeColumnsEqualWidth = false;
        layout.marginWidth = 0;
        selectionArea.setLayout(layout);
        selectionArea.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        // from directory
        createDirectorySelectionArea(selectionArea);

        // from archive
        createArchiveSelectionArea(selectionArea);

        // from directory by default.
        this.itemFromDirectoryRadio.setSelection(true);
        updateSelectionFields(this.itemFromDirectoryRadio.getSelection());
    }

    /**
     * DOC ggu Comment method "createDirectorySelectionArea".
     * 
     * @param selectionArea
     */
    private void createDirectorySelectionArea(Composite selectionArea) {
        this.itemFromDirectoryRadio = new Button(selectionArea, SWT.RADIO);
        this.itemFromDirectoryRadio.setText(Messages.getString("ImportItemsWizardPage_selectDirectoryText")); //$NON-NLS-1$
        setButtonLayoutData(this.itemFromDirectoryRadio);

        this.itemFromDirectoryRadio.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                handleDirectoryRadioSelected();
            }
        });

        this.directoryPathField = new Text(selectionArea, SWT.BORDER);
        this.directoryPathField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL));

        this.directoryPathField.addTraverseListener(new TraverseListener() {

            @Override
            public void keyTraversed(TraverseEvent e) {
                if (e.detail == SWT.TRAVERSE_RETURN) {
                    e.doit = false;
                    updateItemsList(directoryPathField.getText().trim(), true, false);
                }
            }

        });
        this.directoryPathField.addFocusListener(new FocusAdapter() {

            @Override
            public void focusLost(org.eclipse.swt.events.FocusEvent e) {
                updateItemsList(directoryPathField.getText().trim(), true, false);
            }

        });

        this.browseDirectoriesButton = new Button(selectionArea, SWT.PUSH);
        this.browseDirectoriesButton.setText(Messages.getString("ImportItemsWizardPage_browseText")); //$NON-NLS-1$
        setButtonLayoutData(this.browseDirectoriesButton);
        this.browseDirectoriesButton.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                handleBrowseDirectoryButtonPressed();
            }
        });

        // just fill the empty
        new Label(selectionArea, SWT.NONE);
    }

    /**
     * DOC ggu Comment method "createArchiveSelectionArea".
     * 
     * @param selectionArea
     */
    private void createArchiveSelectionArea(Composite selectionArea) {
        this.itemFromArchiveRadio = new Button(selectionArea, SWT.RADIO);
        this.itemFromArchiveRadio.setText(Messages.getString("ImportItemsWizardPage_selectArchiveText")); //$NON-NLS-1$
        setButtonLayoutData(this.itemFromArchiveRadio);

        this.itemFromArchiveRadio.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                handleArchiveRadioSelected();
            }
        });

        this.archivePathField = new Text(selectionArea, SWT.BORDER);
        this.archivePathField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL));

        this.archivePathField.addTraverseListener(new TraverseListener() {

            @Override
            public void keyTraversed(TraverseEvent e) {
                if (e.detail == SWT.TRAVERSE_RETURN) {
                    e.doit = false;
                    updateItemsList(archivePathField.getText().trim(), false, false);
                }
            }
        });

        this.archivePathField.addFocusListener(new FocusAdapter() {

            @Override
            public void focusLost(org.eclipse.swt.events.FocusEvent e) {
                updateItemsList(archivePathField.getText().trim(), false, false);
            }
        });

        this.browseArchivesButton = new Button(selectionArea, SWT.PUSH);
        this.browseArchivesButton.setText(Messages.getString("ImportItemsWizardPage_browseText")); //$NON-NLS-1$
        setButtonLayoutData(this.browseArchivesButton);

        this.browseArchivesButton.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                handleArchiveButtonPressed();
            }
        });

        if (isEnableForExchange()) {
            this.fromExchangeButton = new Button(selectionArea, SWT.PUSH);
            this.fromExchangeButton.setText(Messages.getString("ImportItemsWizardPage_fromExchangeText")); //$NON-NLS-1$
            this.fromExchangeButton.setToolTipText(Messages.getString("ImportItemsWizardPage_fromExchangeToolTipText")); //$NON-NLS-1$
            setButtonLayoutData(fromExchangeButton);

            this.fromExchangeButton.addSelectionListener(new SelectionAdapter() {

                @Override
                public void widgetSelected(SelectionEvent e) {
                    if (GlobalServiceRegister.getDefault().isServiceRegistered(IExchangeService.class)) {
                        archivePathField.setEditable(false);

                        IExchangeService service = (IExchangeService) GlobalServiceRegister.getDefault().getService(
                                IExchangeService.class);

                        String selectedArchive = service.openExchangeDialog();
                        if (selectedArchive != null) {
                            archivePathField.setText(previouslyBrowsedArchivePath);
                            previouslyBrowsedArchivePath = selectedArchive;
                            updateItemsList(selectedArchive, false, false);
                        }
                    } else {
                        MessageDialog.openWarning(getShell(),
                                Messages.getString("ImportItemsWizardPage_fromExchangeWarningTitle"), //$NON-NLS-1$
                                Messages.getString("ImportItemsWizardPage_fromExchangeWarningMessage")); //$NON-NLS-1$
                    }
                }
            });
        }

    }

    private void updateSelectionFields(boolean fromDir) {
        this.directoryPathField.setEnabled(fromDir);
        this.browseDirectoriesButton.setEnabled(fromDir);

        this.archivePathField.setEnabled(!fromDir);
        this.browseArchivesButton.setEnabled(!fromDir);
        if (isEnableForExchange()) {
            this.fromExchangeButton.setEnabled(!fromDir);

        }
    }

    private void createItemListArea(Composite parent) {
        Composite itemsArea = new Composite(parent, SWT.NONE);
        GridLayout layout2 = new GridLayout();
        layout2.numColumns = 2;
        layout2.marginWidth = 0;
        layout2.makeColumnsEqualWidth = false;
        itemsArea.setLayout(layout2);

        GridData gridData = new GridData(GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL | GridData.FILL_BOTH);
        gridData.heightHint = 250;
        gridData.widthHint = 600;
        itemsArea.setLayoutData(gridData);

        createItemsTreeViewer(itemsArea);

        createItemsListButtonsArea(itemsArea);

    }

    private void createItemsTreeViewer(Composite parent) {
        filteredCheckboxTree = new FilteredCheckboxTree(parent, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL | SWT.MULTI);
        CheckboxTreeViewer viewer = filteredCheckboxTree.getViewer();

        viewer.setContentProvider(new ImportItemsViewerContentProvider());
        viewer.setLabelProvider(new ImportItemsViewerLabelProvider());
        viewer.setSorter(new ImportItemsViewerSorter());
        viewer.addFilter(new ImportItemsViewerFilter());
        viewer.addCheckStateListener(new ICheckStateListener() {

            @Override
            public void checkStateChanged(CheckStateChangedEvent event) {
                filteredCheckboxTree.calculateCheckedLeafNodes();
                updateFinishStatus();
            }
        });
        viewer.setInput(nodesBuilder.getProjectNodes());

    }

    private void createItemsListButtonsArea(Composite listComposite) {
        Composite buttonsComposite = new Composite(listComposite, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.marginWidth = 0;
        layout.marginHeight = 25;
        buttonsComposite.setLayout(layout);

        buttonsComposite.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));

        Button refresh = new Button(buttonsComposite, SWT.PUSH);
        refresh.setText(Messages.getString("ImportItemsWizardPage_refreshButtonText")); //$NON-NLS-1$
        refresh.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                if (itemFromDirectoryRadio.getSelection()) {
                    updateItemsList(directoryPathField.getText().trim(), true, true);
                } else {
                    updateItemsList(archivePathField.getText().trim(), false, true);
                }
            }
        });
        setButtonLayoutData(refresh);

        Button selectAll = new Button(buttonsComposite, SWT.PUSH);
        selectAll.setText(Messages.getString("ImportItemsWizardPage_selectButtonText")); //$NON-NLS-1$
        selectAll.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                final CheckboxTreeViewer viewer = filteredCheckboxTree.getViewer();
                if (viewer.getTree().getItemCount() > 0) {
                    for (int i = 0; i < viewer.getTree().getItemCount(); i++) {
                        TreeItem topItem = viewer.getTree().getItem(i)/* .getTopItem() */;
                        if (topItem != null) {
                            viewer.setSubtreeChecked(topItem.getData(), true);
                            filteredCheckboxTree.calculateCheckedLeafNodes();
                            updateFinishStatus();
                        }
                    }
                }
            }
        });
        setButtonLayoutData(selectAll);

        Button deselectAll = new Button(buttonsComposite, SWT.PUSH);
        deselectAll.setText(Messages.getString("ImportItemsWizardPage_deselectAllButtonText")); //$NON-NLS-1$
        deselectAll.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                filteredCheckboxTree.getViewer().setCheckedElements(new Object[0]);
                filteredCheckboxTree.calculateCheckedLeafNodes();
                updateFinishStatus();
            }
        });
        setButtonLayoutData(deselectAll);

        Button expandAll = new Button(buttonsComposite, SWT.PUSH);
        expandAll.setText(Messages.getString("ImportItemsWizardPage_expandAllButtonText")); //$NON-NLS-1$
        expandAll.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                filteredCheckboxTree.getViewer().expandAll();
            }
        });
        setButtonLayoutData(expandAll);

        Button collapseAll = new Button(buttonsComposite, SWT.PUSH);
        collapseAll.setText(Messages.getString("ImportItemsWizardPage_collapseAllButtonText")); //$NON-NLS-1$
        collapseAll.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                filteredCheckboxTree.getViewer().collapseAll();
            }
        });
        setButtonLayoutData(collapseAll);
    }

    private void createErrorsListArea(Composite workArea) {
        Composite composite = new Composite(workArea, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.makeColumnsEqualWidth = false;
        layout.marginWidth = 0;
        composite.setLayout(layout);
        GridData gridData = new GridData(GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL | GridData.FILL_BOTH);
        gridData.heightHint = 100;
        composite.setLayoutData(gridData);

        Label title = new Label(composite, SWT.NONE);
        title.setText(Messages.getString("ImportItemsWizardPage_messagesText")); //$NON-NLS-1$
        title.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        errorsListViewer = new TableViewer(composite, SWT.BORDER);
        errorsListViewer.getControl().setLayoutData(gridData);

        errorsListViewer.setContentProvider(new ArrayContentProvider());
        errorsListViewer.setLabelProvider(new LabelProvider());
        errorsListViewer.setSorter(new ViewerSorter());

    }

    /**
     * DOC ggu Comment method "createAdditionArea".
     * 
     * @param workArea
     */
    private void createAdditionArea(Composite workArea) {
        // see feature 3949
        this.overwriteButton = new Button(workArea, SWT.CHECK);
        this.overwriteButton.setText(Messages.getString("ImportItemsWizardPage_overwriteItemsText")); //$NON-NLS-1$
        this.overwriteButton.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                if (StringUtils.isNotEmpty(directoryPathField.getText()) || StringUtils.isNotEmpty(archivePathField.getText())) {
                    populateItems(overwriteButton.getSelection());
                }
            }

        });
    }

    private boolean isEnableForExchange() {
        return PluginChecker.isExchangeSystemLoaded() && !TalendPropertiesUtil.isHideExchange();
    }

    /**
     * From directory
     * 
     */
    private void handleDirectoryRadioSelected() {
        boolean selection = this.itemFromDirectoryRadio.getSelection();
        updateSelectionFields(selection);
        if (selection) {
            this.directoryPathField.setFocus();
            updateItemsList(this.directoryPathField.getText().trim(), true, false);
        }
    }

    /**
     * From directory
     * 
     */
    private void handleBrowseDirectoryButtonPressed() {
        DirectoryDialog dialog = new DirectoryDialog(this.getShell());
        dialog.setText(Messages.getString("ImportItemsWizardPage_selectDirectoryDialogTitle")); //$NON-NLS-1$
        dialog.setMessage(dialog.getText()); // FIXME

        String dirPath = this.directoryPathField.getText().trim();
        if (dirPath.length() == 0 && previouslyBrowsedDirectoryPath != null) {
            dirPath = previouslyBrowsedDirectoryPath;
        }

        if (dirPath.length() == 0) {
            dialog.setFilterPath(ResourcesPlugin.getWorkspace().getRoot().getLocation().toOSString());
        } else {
            File path = new File(dirPath);
            if (path.exists()) {
                dialog.setFilterPath(new Path(dirPath).toOSString());
            }
        }

        String selectedDirectory = dialog.open();
        if (selectedDirectory != null) {
            this.directoryPathField.setText(selectedDirectory);
            previouslyBrowsedDirectoryPath = selectedDirectory;
            updateItemsList(selectedDirectory, true, false);
        }

    }

    /**
     * From archive
     * 
     */
    private void handleArchiveRadioSelected() {
        boolean selection = this.itemFromArchiveRadio.getSelection();
        updateSelectionFields(!selection);
        if (selection) {
            this.archivePathField.setFocus();
            updateItemsList(this.archivePathField.getText().trim(), false, false);
        }
    }

    /**
     * From archive
     * 
     */
    private void handleArchiveButtonPressed() {
        FileDialog dialog = new FileDialog(archivePathField.getShell());
        dialog.setText(Messages.getString("ImportItemsWizardPage_selectArchiveDialogTitle")); //$NON-NLS-1$
        dialog.setFilterExtensions(ARCHIVE_FILE_MASK);

        String filePath = this.archivePathField.getText().trim();
        if (filePath.length() == 0 && previouslyBrowsedArchivePath != null) {
            filePath = previouslyBrowsedArchivePath;
        }

        if (filePath.length() == 0) {
            dialog.setFilterPath(ResourcesPlugin.getWorkspace().getRoot().getLocation().toOSString());
        } else {
            File file = new File(filePath);
            if (file.exists()) {
                dialog.setFilterPath(new Path(filePath).toOSString());
            }
        }

        String selectedArchive = dialog.open();
        if (selectedArchive != null) {
            this.archivePathField.setText(selectedArchive);
            previouslyBrowsedArchivePath = selectedArchive;
            updateItemsList(selectedArchive, false, false);
        }

    }

    public void updateItemsList(final String path, final boolean fromDir, boolean isneedUpdate) {
        // if not force to update, and same as before path, nothing to do.
        if (!isneedUpdate && path.equals(lastWorkedPath)) {
            return;
        }
        lastWorkedPath = path;

        CheckboxTreeViewer viewer = filteredCheckboxTree.getViewer();

        if (StringUtils.isEmpty(path)) {
            selectedItemRecords.clear();
            viewer.refresh(true);
            // get the top item to check if tree is empty, if not then uncheck everything
            TreeItem topItem = viewer.getTree().getTopItem();
            if (topItem != null) {
                viewer.setSubtreeChecked(topItem.getData(), false);
            } // else not root element, tree is already empty
        } else {
            try {
                getContainer().run(true, true, new IRunnableWithProgress() {

                    @Override
                    public void run(IProgressMonitor monitor) {

                        monitor.beginTask(Messages.getString("ImportItemsWizardPage_searchingMessage"), 100); //$NON-NLS-1$ 
                        monitor.worked(10);

                        File srcFile = new File(path);
                        Object path2Object = srcFile;

                        if (!fromDir && ArchiveFileManipulations.isTarFile(path)) {
                            TarFile sourceTarFile = getSpecifiedTarSourceFile(srcFile);
                            if (sourceTarFile == null) {
                                return;
                            }
                            TarLeveledStructureProvider provider = new TarLeveledStructureProvider(sourceTarFile);
                            resManager = ResourcesManagerFactory.getInstance().createResourcesManager(provider);

                            path2Object = provider.getRoot();
                        } else if (!fromDir && ArchiveFileManipulations.isZipFile(path)) {
                            ZipFile sourceFile = getSpecifiedZipSourceFile(srcFile);
                            if (sourceFile == null) {
                                return;
                            }
                            ZipLeveledStructureProvider provider = new ZipLeveledStructureProvider(sourceFile);
                            resManager = ResourcesManagerFactory.getInstance().createResourcesManager(provider);

                            path2Object = provider.getRoot();
                        } else if (fromDir && srcFile.isDirectory()) {
                            resManager = ResourcesManagerFactory.getInstance().createResourcesManager();
                        }
                        monitor.worked(60);
                        resManager.collectPath2Object(path2Object);

                        monitor.done();
                    }

                });
            } catch (InvocationTargetException e) {
                IDEWorkbenchPlugin.log(e.getMessage(), e);
            } catch (InterruptedException e) {
                // Nothing to do if the user interrupts.
            }

            populateItems(this.overwriteButton.getSelection());
        }

    }

    private void checkValidItemRecords() {
        ItemRecord[] validItems = getValidItemRecords();
        boolean hasValidItems = validItems.length > 0;

        if (!hasValidItems) {
            this.setErrorMessage(Messages.getString("ImportItemsWizardPage_noValidItemsInPathMessage")); //$NON-NLS-1$
        }
        // this.setPageComplete(hasValidItems);
    }

    public ItemRecord[] getValidItemRecords() {

        List<ItemRecord> validItems = new ArrayList<ItemRecord>();
        for (ItemRecord item : this.selectedItemRecords) {
            if (item.isValid()) {
                validItems.add(item);

            }
        }
        return validItems.toArray(new ItemRecord[0]);
    }

    private TarFile getSpecifiedTarSourceFile(File srcFile) {
        if (!srcFile.exists()) {
            return null;
        }

        try {
            return new TarFile(srcFile);
        } catch (TarException e) {
            displayErrorDialog(Messages.getString("ImportItemsWizardPage_TarImport_badFormat")); //$NON-NLS-1$ 
        } catch (IOException e) {
            displayErrorDialog(Messages.getString("ImportItemsWizardPage_couldNotRead")); //$NON-NLS-1$ 
        }

        archivePathField.setFocus();
        return null;
    }

    private ZipFile getSpecifiedZipSourceFile(File srcFile) {
        if (!srcFile.exists()) {
            return null;
        }

        try {
            return new ZipFile(srcFile);
        } catch (ZipException e) {
            displayErrorDialog(Messages.getString("ImportItemsWizardPage_ZipImport_badFormat")); //$NON-NLS-1$ 
        } catch (IOException e) {
            displayErrorDialog(Messages.getString("ImportItemsWizardPage_couldNotRead")); //$NON-NLS-1$ 
        }

        archivePathField.setFocus();
        return null;
    }

    protected void displayErrorDialog(String message) {
        MessageDialog.openError(getContainer().getShell(), Messages.getString("ImportItemsWizardPage_errorTitle"), message); //$NON-NLS-1$ 
    }

    private void populateItems(final boolean overwrite) {
        this.selectedItemRecords.clear();
        // importItemUtil.clearAllData();
        nodesBuilder.clear();
        errors.clear();
        updateErrorListViewer();

        IRunnableWithProgress op = new IRunnableWithProgress() {

            @Override
            public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                // List<ItemRecord> items = importItemUtil.populateItems(resManager, overwrite, monitor);
                List<ItemRecord> items = ImportExportHandlersManager.getInstance().populateImportingItems(resManager, overwrite,
                        monitor);
                nodesBuilder.addItems(items);
            }

        };
        try {
            new ProgressMonitorDialog(getShell()).run(true, true, op);
        } catch (Exception e) {
            // ignore me
        }

        ItemRecord[] allImportItemRecords = nodesBuilder.getAllImportItemRecords();
        for (ItemRecord itemRecord : allImportItemRecords) {
            // bug 21738
            if (itemRecord.getExistingItemWithSameId() != null
                    && itemRecord.getExistingItemWithSameId() instanceof RepositoryViewObject) {
                RepositoryViewObject reObject = (RepositoryViewObject) itemRecord.getExistingItemWithSameId();
                if (itemRecord.getProperty() != null && reObject != null) {
                    if (itemRecord.getProperty().getId().equals(reObject.getId())
                            && itemRecord.getProperty().getLabel().equals(reObject.getLabel())) {
                        for (String error : itemRecord.getErrors()) {
                            errors.add("'" + itemRecord.getItemName() + "' " + error); //$NON-NLS-1$ //$NON-NLS-2$
                        }
                    } else {
                        // TDI-21399,TDI-21401
                        // if item is locked, cannot overwrite
                        ERepositoryStatus status = reObject.getRepositoryStatus();
                        if (status == ERepositoryStatus.LOCK_BY_OTHER || status == ERepositoryStatus.LOCK_BY_USER) {
                            for (String error : itemRecord.getErrors()) {
                                errors.add("'" + itemRecord.getItemName() + "' " + error); //$NON-NLS-1$ //$NON-NLS-2$
                            }
                        }
                    }
                }
            } else {
                if (itemRecord.getProperty() != null) {
                    for (String error : itemRecord.getErrors()) {
                        errors.add("'" + itemRecord.getItemName() + "' " + error); //$NON-NLS-1$ //$NON-NLS-2$
                    }
                }
            }
        }

        updateErrorListViewer();

        selectedItemRecords.addAll(Arrays.asList(allImportItemRecords));

        final CheckboxTreeViewer viewer = this.filteredCheckboxTree.getViewer();
        viewer.setInput(nodesBuilder.getProjectNodes());
        viewer.refresh(true);
        viewer.expandAll();
        filteredCheckboxTree.resetCheckedElements();
        checkValidItemRecords();
        updateFinishStatus();
    }

    private void updateFinishStatus() {
        List<ItemRecord> checkedElements = getCheckedElements();
        updateErrorMessage(checkedElements);
        if (checkedElements.isEmpty() || getErrorMessage() != null) {
            this.setPageComplete(false);
        } else {
            this.setPageComplete(true);
        }
    }

    private void updateErrorListViewer() {
        errorsListViewer.setInput(errors);
        errorsListViewer.refresh();
    }

    private List<ItemRecord> getCheckedElements() {
        // add this if user use filter
        Set<ItemImportNode> checkedElements = new HashSet<ItemImportNode>();
        for (Object obj : filteredCheckboxTree.getCheckedLeafNodes()) {
            if (obj instanceof ItemImportNode) {
                checkedElements.add((ItemImportNode) obj);
            }
        }
        // add this if user does not use filter
        for (Object obj : filteredCheckboxTree.getViewer().getCheckedElements()) {
            if (obj instanceof ItemImportNode) {
                checkedElements.add((ItemImportNode) obj);
            }
        }
        // sort the item
        List<ItemImportNode> list = new ArrayList<ItemImportNode>(checkedElements);
        Collections.sort(list);

        List<ItemRecord> items = new ArrayList<ItemRecord>(list.size());
        for (ItemImportNode node : list) {
            items.add(node.getItemRecord());
        }
        return items;
    }

    /**
     * Checks for consistency in selected elements and report an error message. in case of error or null the message
     * error.
     * 
     * @param checkedElements element to be checked
     */
    private void updateErrorMessage(List<ItemRecord> checkedElements) {
        String errorMessage = checkErrorFor2ItemsWithSameIdAndVersion(checkedElements);
        setErrorMessage(errorMessage);
    }

    /**
     * This check that 2 items in the list do not have the same Id and the same version. if that is so the return an
     * error message else return null.
     * 
     * @param checkedElementsn the element to be checked
     * @return an error message or null if no error.
     */
    private String checkErrorFor2ItemsWithSameIdAndVersion(List<ItemRecord> checkedElements) {
        String errorMessage = null;
        HashMap<String, ItemRecord> duplicateCheckMap = new HashMap<String, ItemRecord>();
        for (ItemRecord itRecord : checkedElements) {
            ItemRecord otherRecord = duplicateCheckMap.put(itRecord.getProperty().getId() + itRecord.getProperty().getVersion(),
                    itRecord);
            if (otherRecord != null) {
                errorMessage = Messages.getString(
                        "ImportItemsWizardPage_sameIdProblemMessage", itRecord.getPath(), otherRecord.getPath()); //$NON-NLS-1$
            }// else keep going
        }
        return errorMessage;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.wizard.WizardPage#isPageComplete()
     */
    @Override
    public boolean isPageComplete() {
        if (selectedItemRecords.isEmpty() || getErrorMessage() != null) {
            return false;
        }
        return super.isPageComplete();
    }

    public boolean performCancel() {
        selectedItemRecords.clear();
        nodesBuilder.clear();
        return true;
    }

    public boolean performFinish() {
        // final List<ItemRecord> itemRecords = new ArrayList<ItemRecord>();
        final List<ItemRecord> checkedItemRecords = getCheckedElements();

        // ?????? why need unlock the items?
        for (ItemRecord itemRecord : checkedItemRecords) {
            Item item = itemRecord.getProperty().getItem();
            IProxyRepositoryFactory factory = ProxyRepositoryFactory.getInstance();
            if (item.getState().isLocked()) {
                try {
                    factory.unlock(item);
                } catch (PersistenceException e) {
                    ExceptionHandler.process(e);
                } catch (LoginException e) {
                    ExceptionHandler.process(e);
                }
            }
            ERepositoryStatus status = factory.getStatus(item);
            if (status != null && status == ERepositoryStatus.LOCK_BY_USER) {
                try {
                    factory.unlock(item);
                } catch (PersistenceException e) {
                    ExceptionHandler.process(e);
                } catch (LoginException e) {
                    ExceptionHandler.process(e);
                }
            }
        }

        final boolean overwrite = overwriteButton.getSelection();
        try {
            IRunnableWithProgress iRunnableWithProgress = new IRunnableWithProgress() {

                @Override
                public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                    IPath destinationPath = null;
                    String contentType = "";
                    // TODO
                    // if (rNode != null && rNode.getType().equals(ENodeType.SIMPLE_FOLDER)) {
                    // destinationPath = RepositoryNodeUtilities.getPath(rNode);
                    // contentType = rNode.getContentType().name();
                    // }
                    ImportExportHandlersManager.getInstance().importItemRecords(monitor, resManager, checkedItemRecords,
                            overwrite, null, null, nodesBuilder.getAllImportItemRecords());

                }
            };

            new ProgressMonitorDialog(getShell()).run(true, true, iRunnableWithProgress);

        } catch (Exception e) {
            ExceptionHandler.process(e);
        }
        // clean
        if (resManager instanceof ProviderManager) {
            ((ProviderManager) resManager).closeResource();
        }
        checkedItemRecords.clear();
        nodesBuilder.clear();
        return true;
    }

    private Set<ItemRecord> getHadoopSubrecords(List<ItemRecord> itemRecords) {
        Set<ItemRecord> hadoopSubrecords = new HashSet<ItemRecord>();
        for (ItemRecord itemRecord : itemRecords) {
            hadoopSubrecords.addAll(ImportItemUtil.collectHadoopSubrecords(resManager, nodesBuilder.getAllImportItemRecords(),
                    itemRecord));
        }

        return hadoopSubrecords;
    }
}
