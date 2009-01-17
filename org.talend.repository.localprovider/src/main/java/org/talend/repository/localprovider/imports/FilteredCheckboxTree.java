// ============================================================================
//
// Copyright (C) 2006-2007 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.repository.localprovider.imports;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.accessibility.AccessibleAdapter;
import org.eclipse.swt.accessibility.AccessibleEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IWorkbenchPreferenceConstants;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ContainerCheckedTreeViewer;
import org.eclipse.ui.internal.WorkbenchMessages;
import org.eclipse.ui.internal.misc.StringMatcher;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.progress.WorkbenchJob;

/**
 * Modification from eclipse built-in class FilteredTree. A simple control that provides a text widget and a tree
 * viewer. The contents of the text widget are used to drive a PatternFilter that is on the viewer.
 * 
 */
public class FilteredCheckboxTree extends Composite {

    /**
     * The filter text widget to be used by this tree. This value may be <code>null</code> if there is no filter widget,
     * or if the controls have not yet been created.
     */
    protected Text filterText;

    /**
     * The control representing the clear button for the filter text entry. This value may be <code>null</code> if no
     * such button exists, or if the controls have not yet been created.
     */
    protected ToolBarManager filterToolBar;

    /**
     * The viewer for the filtered tree. This value should never be <code>null</code> after the widget creation methods
     * are complete.
     */
    protected CheckboxTreeViewer treeViewer;

    /**
     * The Composite on which the filter controls are created. This is used to set the background color of the filter
     * controls to match the surrounding controls.
     */
    protected Composite filterComposite;

    /**
     * The pattern filter for the tree. This value must not be <code>null</code>.
     */
    private PatternFilter patternFilter;

    /**
     * The text to initially show in the filter text control.
     */
    protected String initialText = ""; //$NON-NLS-1$

    /**
     * The job used to refresh the tree.
     */
    private Job refreshJob;

    /**
     * The parent composite of the filtered tree.
     * 
     * @since 3.3
     */
    protected Composite parent;

    /**
     * Whether or not to show the filter controls (text and clear button). The default is to show these controls. This
     * can be overridden by providing a setting in the product configuration file. The setting to add to not show these
     * controls is:
     * 
     * org.eclipse.ui/SHOW_FILTERED_TEXTS=false
     */
    protected boolean showFilterControls;

    /**
     * @since 3.3
     */
    protected Composite treeComposite;

    /**
     * Image descriptor for enabled clear button.
     */
    private static final String CLEAR_ICON = "org.eclipse.ui.internal.dialogs.CLEAR_ICON"; //$NON-NLS-1$

    /**
     * Image descriptor for disabled clear button.
     */
    private static final String DCLEAR_ICON = "org.eclipse.ui.internal.dialogs.DCLEAR_ICON"; //$NON-NLS-1$

    /**
     * Maximum time spent expanding the tree after the filter text has been updated (this is only used if we were able
     * to at least expand the visible nodes)
     */
    private static final long SOFT_MAX_EXPAND_TIME = 200;

    /**
     * Get image descriptors for the clear button.
     */
    static {
        ImageDescriptor descriptor = AbstractUIPlugin.imageDescriptorFromPlugin(PlatformUI.PLUGIN_ID,
                "$nl$/icons/full/etool16/clear_co.gif"); //$NON-NLS-1$
        if (descriptor != null) {
            JFaceResources.getImageRegistry().put(CLEAR_ICON, descriptor);
        }
        descriptor = AbstractUIPlugin.imageDescriptorFromPlugin(PlatformUI.PLUGIN_ID, "$nl$/icons/full/dtool16/clear_co.gif"); //$NON-NLS-1$
        if (descriptor != null) {
            JFaceResources.getImageRegistry().put(DCLEAR_ICON, descriptor);
        }
    }

    public FilteredCheckboxTree(Composite parent, int treeStyle) {
        this(parent, SWT.NONE, new PatternFilter());
    }

    /**
     * Create a new instance of the receiver.
     * 
     * @param parent the parent <code>Composite</code>
     * @param treeStyle the style bits for the <code>Tree</code>
     * @param filter the filter to be used
     */
    public FilteredCheckboxTree(Composite parent, int treeStyle, PatternFilter filter) {
        super(parent, SWT.NONE);
        this.parent = parent;
        init(treeStyle, filter);
    }

    /**
     * Create a new instance of the receiver. Subclasses that wish to override the default creation behavior may use
     * this constructor, but must ensure that the <code>init(composite, int, PatternFilter)</code> method is called in
     * the overriding constructor.
     * 
     * @param parent the parent <code>Composite</code>
     * @see #init(int, PatternFilter)
     * 
     * @since 3.3
     */
    protected FilteredCheckboxTree(Composite parent) {
        super(parent, SWT.NONE);
        this.parent = parent;
    }

    /**
     * Create the filtered tree.
     * 
     * @param treeStyle the style bits for the <code>Tree</code>
     * @param filter the filter to be used
     * 
     * @since 3.3
     */
    protected void init(int treeStyle, PatternFilter filter) {
        patternFilter = filter;
        showFilterControls = PlatformUI.getPreferenceStore().getBoolean(IWorkbenchPreferenceConstants.SHOW_FILTERED_TEXTS);
        createControl(parent, treeStyle);
        createRefreshJob();
        setInitialText(WorkbenchMessages.FilteredTree_FilterMessage);
        setFont(parent.getFont());
    }

    /**
     * Create the filtered tree's controls. Subclasses should override.
     * 
     * @param parent
     * @param treeStyle
     */
    protected void createControl(Composite parent, int treeStyle) {
        GridLayout layout = new GridLayout();
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        setLayout(layout);
        setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        if (showFilterControls) {
            filterComposite = new Composite(this, SWT.NONE);
            GridLayout filterLayout = new GridLayout(2, false);
            filterLayout.marginHeight = 0;
            filterLayout.marginWidth = 0;
            filterComposite.setLayout(filterLayout);
            filterComposite.setFont(parent.getFont());

            createFilterControls(filterComposite);
            filterComposite.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
        }

        treeComposite = new Composite(this, SWT.NONE);
        GridLayout treeCompositeLayout = new GridLayout();
        treeCompositeLayout.marginHeight = 0;
        treeCompositeLayout.marginWidth = 0;
        treeComposite.setLayout(treeCompositeLayout);
        GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
        treeComposite.setLayoutData(data);
        createTreeControl(treeComposite, treeStyle);
    }

    /**
     * Create the filter controls. By default, a text and corresponding tool bar button that clears the contents of the
     * text is created. Subclasses may override.
     * 
     * @param parent parent <code>Composite</code> of the filter controls
     * @return the <code>Composite</code> that contains the filter controls
     */
    protected Composite createFilterControls(Composite parent) {
        createFilterText(parent);
        createClearText(parent);
        if (filterToolBar != null) {
            filterToolBar.update(false);
            // initially there is no text to clear
            filterToolBar.getControl().setVisible(false);
        }
        return parent;
    }

    /**
     * Creates and set up the tree and tree viewer. This method calls {@link #doCreateTreeViewer(Composite, int)} to
     * create the tree viewer. Subclasses should override {@link #doCreateTreeViewer(Composite, int)} instead of
     * overriding this method.
     * 
     * @param parent parent <code>Composite</code>
     * @param style SWT style bits used to create the tree
     * @return the tree
     */
    protected Control createTreeControl(Composite parent, int style) {
        treeViewer = doCreateTreeViewer(parent, style);
        GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
        treeViewer.getControl().setLayoutData(data);
        treeViewer.getControl().addDisposeListener(new DisposeListener() {

            /*
             * (non-Javadoc)
             * 
             * @see org.eclipse.swt.events.DisposeListener#widgetDisposed(org.eclipse.swt.events.DisposeEvent)
             */
            public void widgetDisposed(DisposeEvent e) {
                refreshJob.cancel();
            }
        });
        if (treeViewer instanceof NotifyingTreeViewer) {
            patternFilter.setUseCache(true);
        }
        treeViewer.addFilter(patternFilter);
        return treeViewer.getControl();
    }

    /**
     * Creates the tree viewer. Subclasses may override.
     * 
     * @param parent the parent composite
     * @param style SWT style bits used to create the tree viewer
     * @return the tree viewer
     * 
     * @since 3.3
     */
    protected CheckboxTreeViewer doCreateTreeViewer(Composite parent, int style) {
        return new NotifyingTreeViewer(parent, style);
    }

    /**
     * Return the first item in the tree that matches the filter pattern.
     * 
     * @param items
     * @return the first matching TreeItem
     */
    private TreeItem getFirstMatchingItem(TreeItem[] items) {
        for (int i = 0; i < items.length; i++) {
            if (patternFilter.isLeafMatch(treeViewer, items[i].getData())
                    && patternFilter.isElementSelectable(items[i].getData())) {
                return items[i];
            }
            return getFirstMatchingItem(items[i].getItems());
        }
        return null;
    }

    /**
     * Create the refresh job for the receiver.
     * 
     */
    private void createRefreshJob() {
        refreshJob = new WorkbenchJob("Refresh Filter") {//$NON-NLS-1$

            /*
             * (non-Javadoc)
             * 
             * @see org.eclipse.ui.progress.UIJob#runInUIThread(org.eclipse.core.runtime.IProgressMonitor)
             */
            @Override
            public IStatus runInUIThread(IProgressMonitor monitor) {
                if (treeViewer.getControl().isDisposed()) {
                    return Status.CANCEL_STATUS;
                }

                String text = getFilterString();
                if (text == null) {
                    return Status.OK_STATUS;
                }

                boolean initial = initialText != null && initialText.equals(text);
                if (initial) {
                    patternFilter.setPattern(null);
                } else if (text != null) {
                    patternFilter.setPattern(text);
                }

                Control redrawFalseControl = treeComposite != null ? treeComposite : treeViewer.getControl();
                try {
                    // don't want the user to see updates that will be made to the tree
                    // we are setting redraw(false) on the composite to avoid dancing scrollbar
                    redrawFalseControl.setRedraw(false);
                    // if (!narrowingDown) {
                    // // collapse all
                    // TreeItem[] is = treeViewer.getTree().getItems();
                    // for (int i = 0; i < is.length; i++) {
                    // TreeItem item = is[i];
                    // if (item.getExpanded()) {
                    // treeViewer.setExpandedState(item.getData(), false);
                    // }
                    // }
                    // }
                    treeViewer.refresh(true);

                    if (text.length() > 0 && !initial) {
                        /*
                         * Expand elements one at a time. After each is expanded, check to see if the filter text has
                         * been modified. If it has, then cancel the refresh job so the user doesn't have to endure
                         * expansion of all the nodes.
                         */
                        TreeItem[] items = getViewer().getTree().getItems();
                        int treeHeight = getViewer().getTree().getBounds().height;
                        int numVisibleItems = treeHeight / getViewer().getTree().getItemHeight();
                        long stopTime = SOFT_MAX_EXPAND_TIME + System.currentTimeMillis();
                        if (items.length > 0 && recursiveExpand(items, monitor, stopTime, new int[] { numVisibleItems })) {
                            return Status.CANCEL_STATUS;
                        }

                        // enabled toolbar - there is text to clear
                        // and the list is currently being filtered
                        updateToolbar(true);
                    } else {
                        // disabled toolbar - there is no text to clear
                        // and the list is currently not filtered
                        updateToolbar(false);
                    }
                } finally {
                    // done updating the tree - set redraw back to true
                    TreeItem[] items = getViewer().getTree().getItems();
                    if (items.length > 0 && getViewer().getTree().getSelectionCount() == 0) {
                        treeViewer.getTree().setTopItem(items[0]);
                    }
                    redrawFalseControl.setRedraw(true);
                    if (!initial) {
                        refreshCompleted();
                    }
                }
                return Status.OK_STATUS;
            }

            /**
             * Returns true if the job should be canceled (because of timeout or actual cancellation).
             * 
             * @param items
             * @param provider
             * @param monitor
             * @param cancelTime
             * @param numItemsLeft
             * @return true if canceled
             */
            private boolean recursiveExpand(TreeItem[] items, IProgressMonitor monitor, long cancelTime, int[] numItemsLeft) {
                boolean canceled = false;
                for (int i = 0; !canceled && i < items.length; i++) {
                    TreeItem item = items[i];
                    boolean visible = numItemsLeft[0]-- >= 0;
                    if (monitor.isCanceled() || (!visible && System.currentTimeMillis() > cancelTime)) {
                        canceled = true;
                    } else {
                        Object itemData = item.getData();
                        if (itemData != null) {
                            if (!item.getExpanded()) {
                                // do the expansion through the viewer so that it can refresh children appropriately.
                                treeViewer.setExpandedState(itemData, true);
                            }
                            TreeItem[] children = item.getItems();
                            if (items.length > 0) {
                                canceled = recursiveExpand(children, monitor, cancelTime, numItemsLeft);
                            }
                        }
                    }
                }
                return canceled;
            }

        };
        refreshJob.setSystem(true);
    }

    protected void updateToolbar(boolean visible) {
        if (filterToolBar != null) {
            filterToolBar.getControl().setVisible(visible);
        }
    }

    /**
     * Creates the filter text and adds listeners. This method calls {@link #doCreateFilterText(Composite)} to create
     * the text control. Subclasses should override {@link #doCreateFilterText(Composite)} instead of overriding this
     * method.
     * 
     * @param parent <code>Composite</code> of the filter text
     */
    protected void createFilterText(Composite parent) {
        filterText = doCreateFilterText(parent);
        filterText.getAccessible().addAccessibleListener(new AccessibleAdapter() {

            /*
             * (non-Javadoc)
             * 
             * @see
             * org.eclipse.swt.accessibility.AccessibleListener#getName(org.eclipse.swt.accessibility.AccessibleEvent)
             */
            @Override
            public void getName(AccessibleEvent e) {
                String filterTextString = filterText.getText();
                if (filterTextString.length() == 0) {
                    e.result = initialText;
                } else {
                    e.result = filterTextString;
                }
            }
        });

        filterText.addFocusListener(new FocusAdapter() {

            /*
             * (non-Javadoc)
             * 
             * @see org.eclipse.swt.events.FocusListener#focusLost(org.eclipse.swt.events.FocusEvent)
             */
            @Override
            public void focusGained(FocusEvent e) {
                /*
                 * Running in an asyncExec because the selectAll() does not appear to work when using mouse to give
                 * focus to text.
                 */
                Display display = filterText.getDisplay();
                display.asyncExec(new Runnable() {

                    public void run() {
                        if (!filterText.isDisposed()) {
                            if (getInitialText().equals(filterText.getText().trim())) {
                                filterText.selectAll();
                            }
                        }
                    }
                });
            }
        });

        filterText.addKeyListener(new KeyAdapter() {

            /*
             * (non-Javadoc)
             * 
             * @see org.eclipse.swt.events.KeyAdapter#keyReleased(org.eclipse.swt.events.KeyEvent)
             */
            @Override
            public void keyPressed(KeyEvent e) {
                // on a CR we want to transfer focus to the list
                boolean hasItems = getViewer().getTree().getItemCount() > 0;
                if (hasItems && e.keyCode == SWT.ARROW_DOWN) {
                    treeViewer.getTree().setFocus();
                } else if (e.character == SWT.CR) {
                    return;
                }
            }
        });

        // enter key set focus to tree
        filterText.addTraverseListener(new TraverseListener() {

            public void keyTraversed(TraverseEvent e) {
                if (e.detail == SWT.TRAVERSE_RETURN) {
                    e.doit = false;
                    if (getViewer().getTree().getItemCount() == 0) {
                        Display.getCurrent().beep();
                    } else {
                        // if the initial filter text hasn't changed, do not try to match
                        boolean hasFocus = getViewer().getTree().setFocus();
                        boolean textChanged = !getInitialText().equals(filterText.getText().trim());
                        if (hasFocus && textChanged && filterText.getText().trim().length() > 0) {
                            TreeItem item = getFirstMatchingItem(getViewer().getTree().getItems());
                            if (item != null) {
                                getViewer().getTree().setSelection(new TreeItem[] { item });
                                ISelection sel = getViewer().getSelection();
                                getViewer().setSelection(sel, true);
                            }
                        }
                    }
                }
            }
        });

        filterText.addModifyListener(new ModifyListener() {

            /*
             * (non-Javadoc)
             * 
             * @see org.eclipse.swt.events.ModifyListener#modifyText(org.eclipse.swt.events.ModifyEvent)
             */
            public void modifyText(ModifyEvent e) {
                textChanged();
            }
        });

        GridData gridData = new GridData(SWT.FILL, SWT.BEGINNING, true, false);
        // if the text widget supported cancel then it will have it's own
        // integrated button. We can take all of the space.
        if ((filterText.getStyle() & SWT.CANCEL) != 0) {
            gridData.horizontalSpan = 2;
        }
        filterText.setLayoutData(gridData);
    }

    /**
     * Creates the text control for entering the filter text. Subclasses may override.
     * 
     * @param parent the parent composite
     * @return the text widget
     * 
     * @since 3.3
     */
    protected Text doCreateFilterText(Composite parent) {
        return new Text(parent, SWT.SINGLE | SWT.BORDER | SWT.SEARCH | SWT.CANCEL);
    }

    private String previousFilterText;

    // private boolean narrowingDown;

    private boolean rememberCheckedItems;

    private Set checkedElements = new HashSet();

    /**
     * Update the receiver after the text has changed.
     */
    protected void textChanged() {
        saveCheckedLeafNodes();
        // narrowingDown = previousFilterText==null || getFilterString().startsWith(previousFilterText);
        previousFilterText = getFilterString();
        // cancel currently running job first, to prevent unnecessary redraw
        refreshJob.cancel();
        refreshJob.schedule(200);
    }

    @SuppressWarnings("unchecked")
    private void saveCheckedLeafNodes() {
        TreeItem[] roots = getViewer().getTree().getItems();
        Set checked = new HashSet();
        Set unchecked = new HashSet();
        collectLeafNodes(roots, checked, unchecked);
        checkedElements.removeAll(unchecked);
        checkedElements.addAll(checked);
    }

    private void collectLeafNodes(TreeItem[] roots, Set checked, Set unchecked) {
        for (TreeItem item : roots) {
            TreeItem[] children = item.getItems();
            if (children == null || children.length == 0) {
                // leaf node
                Object obj = item.getData();
                if (item.getChecked()) {
                    checked.add(obj);
                } else {
                    unchecked.add(obj);
                }
            } else {
                // to handle some special case, such as database connection
                if (isNodeCollectable(item)) {
                    Object obj = item.getData();
                    if (item.getChecked()) {
                        checked.add(obj);
                    } else {
                        unchecked.add(obj);
                    }
                }
                // collect children of this container node
                collectLeafNodes(children, checked, unchecked);
            }
        }
    }

    /**
     * DOC hcw Comment method "isNodeCollectable".
     * 
     * @param item
     * @return
     */
    protected boolean isNodeCollectable(TreeItem item) {
        return false;
    }

    protected void refreshCompleted() {
        getViewer().expandAll();
        restoreCheckedElements();
    }

    protected void restoreCheckedElements() {
        // restore checked leaf nodes
        Object[] checked = getCheckedLeafNodes();
        getViewer().setCheckedElements(checked);
    }

    public Object[] getCheckedLeafNodes() {
        return checkedElements.toArray();
    }

    /**
     * Set the background for the widgets that support the filter text area.
     * 
     * @param background background <code>Color</code> to set
     */
    @Override
    public void setBackground(Color background) {
        super.setBackground(background);
        if (filterComposite != null) {
            filterComposite.setBackground(background);
        }
        if (filterToolBar != null && filterToolBar.getControl() != null) {
            filterToolBar.getControl().setBackground(background);
        }
    }

    /**
     * Create the button that clears the text.
     * 
     * @param parent parent <code>Composite</code> of toolbar button
     */
    private void createClearText(Composite parent) {
        // only create the button if the text widget doesn't support one natively
        if ((filterText.getStyle() & SWT.CANCEL) == 0) {
            filterToolBar = new ToolBarManager(SWT.FLAT | SWT.HORIZONTAL);
            filterToolBar.createControl(parent);

            IAction clearTextAction = new Action("", IAction.AS_PUSH_BUTTON) {//$NON-NLS-1$

                /*
                 * (non-Javadoc)
                 * 
                 * @see org.eclipse.jface.action.Action#run()
                 */
                @Override
                public void run() {
                    clearText();
                }
            };

            clearTextAction.setToolTipText(WorkbenchMessages.FilteredTree_ClearToolTip);
            clearTextAction.setImageDescriptor(JFaceResources.getImageRegistry().getDescriptor(CLEAR_ICON));
            clearTextAction.setDisabledImageDescriptor(JFaceResources.getImageRegistry().getDescriptor(DCLEAR_ICON));

            filterToolBar.add(clearTextAction);
        }
    }

    /**
     * Clears the text in the filter text widget. Also removes the optional additional filter that is provided via
     * addFilter(ViewerFilter).
     */
    protected void clearText() {
        setFilterText(""); //$NON-NLS-1$
        textChanged();
    }

    /**
     * Set the text in the filter control.
     * 
     * @param string
     */
    protected void setFilterText(String string) {
        if (filterText != null) {
            filterText.setText(string);
            selectAll();
        }
    }

    /**
     * Returns the pattern filter used by this tree.
     * 
     * @return The pattern filter; never <code>null</code>.
     */
    public final PatternFilter getPatternFilter() {
        return patternFilter;
    }

    /**
     * Get the tree viewer of the receiver.
     * 
     * @return the tree viewer
     */
    public CheckboxTreeViewer getViewer() {
        return treeViewer;
    }

    /**
     * Get the filter text for the receiver, if it was created. Otherwise return <code>null</code>.
     * 
     * @return the filter Text, or null if it was not created
     */
    public Text getFilterControl() {
        return filterText;
    }

    /**
     * Convenience method to return the text of the filter control. If the text widget is not created, then null is
     * returned.
     * 
     * @return String in the text, or null if the text does not exist
     */
    protected String getFilterString() {
        return filterText != null ? filterText.getText() : null;
    }

    /**
     * Set the text that will be shown until the first focus. A default value is provided, so this method only need be
     * called if overriding the default initial text is desired.
     * 
     * @param text initial text to appear in text field
     */
    public void setInitialText(String text) {
        initialText = text;
        setFilterText(initialText);
        textChanged();
    }

    /**
     * Select all text in the filter text field.
     * 
     */
    protected void selectAll() {
        if (filterText != null) {
            filterText.selectAll();
        }
    }

    /**
     * Get the initial text for the receiver.
     * 
     * @return String
     */
    protected String getInitialText() {
        return initialText;
    }

    /**
     * Return a bold font if the given element matches the given pattern. Clients can opt to call this method from a
     * Viewer's label provider to get a bold font for which to highlight the given element in the tree.
     * 
     * @param element element for which a match should be determined
     * @param tree FilteredTree in which the element resides
     * @param filter PatternFilter which determines a match
     * 
     * @return bold font
     */
    public static Font getBoldFont(Object element, FilteredCheckboxTree tree, PatternFilter filter) {
        String filterText = tree.getFilterString();

        if (filterText == null) {
            return null;
        }

        // Do nothing if it's empty string
        String initialText = tree.getInitialText();
        if (!("".equals(filterText) || initialText.equals(filterText))) {//$NON-NLS-1$
            boolean initial = initialText != null && initialText.equals(filterText);
            if (initial) {
                filter.setPattern(null);
            } else if (filterText != null) {
                filter.setPattern(filterText);
            }

            if (filter.isElementVisible(tree.getViewer(), element) && filter.isLeafMatch(tree.getViewer(), element)) {
                return JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT);
            }
        }
        return null;
    }

    /**
     * Custom tree viewer subclass that clears the caches in patternFilter on any change to the tree. See bug 187200.
     * 
     * @since 3.3
     * 
     */
    class NotifyingTreeViewer extends ContainerCheckedTreeViewer {

        /**
         * @param parent
         * @param style
         */
        public NotifyingTreeViewer(Composite parent, int style) {
            super(parent, style);
        }

        @Override
        public void add(Object parentElementOrTreePath, Object childElement) {
            getPatternFilter().clearCaches();
            super.add(parentElementOrTreePath, childElement);
        }

        @Override
        public void add(Object parentElementOrTreePath, Object[] childElements) {
            getPatternFilter().clearCaches();
            super.add(parentElementOrTreePath, childElements);
        }

        @Override
        protected void inputChanged(Object input, Object oldInput) {
            getPatternFilter().clearCaches();
            super.inputChanged(input, oldInput);
        }

        @Override
        public void insert(Object parentElementOrTreePath, Object element, int position) {
            getPatternFilter().clearCaches();
            super.insert(parentElementOrTreePath, element, position);
        }

        @Override
        public void refresh() {
            getPatternFilter().clearCaches();
            super.refresh();
        }

        @Override
        public void refresh(boolean updateLabels) {
            getPatternFilter().clearCaches();
            super.refresh(updateLabels);
        }

        @Override
        public void refresh(Object element) {
            getPatternFilter().clearCaches();
            super.refresh(element);
        }

        @Override
        public void refresh(Object element, boolean updateLabels) {
            getPatternFilter().clearCaches();
            super.refresh(element, updateLabels);
        }

        @Override
        public void remove(Object elementsOrTreePaths) {
            getPatternFilter().clearCaches();
            super.remove(elementsOrTreePaths);
        }

        @Override
        public void remove(Object parent, Object[] elements) {
            getPatternFilter().clearCaches();
            super.remove(parent, elements);
        }

        @Override
        public void remove(Object[] elementsOrTreePaths) {
            getPatternFilter().clearCaches();
            super.remove(elementsOrTreePaths);
        }

        @Override
        public void replace(Object parentElementOrTreePath, int index, Object element) {
            getPatternFilter().clearCaches();
            super.replace(parentElementOrTreePath, index, element);
        }

        @Override
        public void setChildCount(Object elementOrTreePath, int count) {
            getPatternFilter().clearCaches();
            super.setChildCount(elementOrTreePath, count);
        }

        @Override
        public void setContentProvider(IContentProvider provider) {
            getPatternFilter().clearCaches();
            super.setContentProvider(provider);
        }

        @Override
        public void setHasChildren(Object elementOrTreePath, boolean hasChildren) {
            getPatternFilter().clearCaches();
            super.setHasChildren(elementOrTreePath, hasChildren);
        }

    }

    /**
     * 
     * DOC hcw FilteredCheckboxTree class global comment. Detailled comment
     */
    static class PatternFilter extends ViewerFilter {

        /*
         * Cache of filtered elements in the tree
         */
        private Map cache = new HashMap();

        /*
         * Maps parent elements to TRUE or FALSE
         */
        private Map foundAnyCache = new HashMap();

        private boolean useCache = false;

        /**
         * Whether to include a leading wildcard for all provided patterns. A trailing wildcard is always included.
         */
        private boolean includeLeadingWildcard = false;

        /**
         * The string pattern matcher used for this pattern filter.
         */
        private StringMatcher matcher;

        private boolean useEarlyReturnIfMatcherIsNull = true;

        private final static Object[] EMPTY = new Object[0];

        /*
         * (non-Javadoc)
         * 
         * @see org.eclipse.jface.viewers.ViewerFilter#filter(org.eclipse.jface.viewers.Viewer, java.lang.Object,
         * java.lang.Object[])
         */
        @Override
        public final Object[] filter(Viewer viewer, Object parent, Object[] elements) {
            // we don't want to optimize if we've extended the filter ... this
            // needs to be addressed in 3.4
            // https://bugs.eclipse.org/bugs/show_bug.cgi?id=186404
            if (matcher == null && useEarlyReturnIfMatcherIsNull) {
                return elements;
            }

            if (!useCache) {
                return super.filter(viewer, parent, elements);
            }

            Object[] filtered = (Object[]) cache.get(parent);
            if (filtered == null) {
                Boolean foundAny = (Boolean) foundAnyCache.get(parent);
                if (foundAny != null && !foundAny.booleanValue()) {
                    filtered = EMPTY;
                } else {
                    filtered = super.filter(viewer, parent, elements);
                }
                cache.put(parent, filtered);
            }
            return filtered;
        }

        /**
         * Returns true if any of the elements makes it through the filter. This method uses caching if enabled; the
         * computation is done in computeAnyVisible.
         * 
         * @param viewer
         * @param parent
         * @param elements the elements (must not be an empty array)
         * @return true if any of the elements makes it through the filter.
         */
        private boolean isAnyVisible(Viewer viewer, Object parent, Object[] elements) {
            if (matcher == null) {
                return true;
            }

            if (!useCache) {
                return computeAnyVisible(viewer, elements);
            }

            Object[] filtered = (Object[]) cache.get(parent);
            if (filtered != null) {
                return filtered.length > 0;
            }
            Boolean foundAny = (Boolean) foundAnyCache.get(parent);
            if (foundAny == null) {
                foundAny = computeAnyVisible(viewer, elements) ? Boolean.TRUE : Boolean.FALSE;
                foundAnyCache.put(parent, foundAny);
            }
            return foundAny.booleanValue();
        }

        /**
         * Returns true if any of the elements makes it through the filter.
         * 
         * @param viewer
         * @param elements
         * @return
         */
        private boolean computeAnyVisible(Viewer viewer, Object[] elements) {
            boolean elementFound = false;
            for (int i = 0; i < elements.length && !elementFound; i++) {
                Object element = elements[i];
                elementFound = isElementVisible(viewer, element);
            }
            return elementFound;
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.eclipse.jface.viewers.ViewerFilter#select(org.eclipse.jface.viewers.Viewer, java.lang.Object,
         * java.lang.Object)
         */
        @Override
        public final boolean select(Viewer viewer, Object parentElement, Object element) {
            return isElementVisible(viewer, element);
        }

        /**
         * Sets whether a leading wildcard should be attached to each pattern string.
         * 
         * @param includeLeadingWildcard Whether a leading wildcard should be added.
         */
        public final void setIncludeLeadingWildcard(final boolean includeLeadingWildcard) {
            this.includeLeadingWildcard = includeLeadingWildcard;
        }

        /**
         * The pattern string for which this filter should select elements in the viewer.
         * 
         * @param patternString
         */
        public void setPattern(String patternString) {
            // these 2 strings allow the PatternFilter to be extended in
            // 3.3 - https://bugs.eclipse.org/bugs/show_bug.cgi?id=186404
            if ("org.eclipse.ui.keys.optimization.true".equals(patternString)) { //$NON-NLS-1$
                useEarlyReturnIfMatcherIsNull = true;
                return;
            } else if ("org.eclipse.ui.keys.optimization.false".equals(patternString)) { //$NON-NLS-1$
                useEarlyReturnIfMatcherIsNull = false;
                return;
            }
            clearCaches();
            if (patternString == null || patternString.equals("")) { //$NON-NLS-1$
                matcher = null;
            } else {
                String pattern = patternString + "*"; //$NON-NLS-1$
                if (includeLeadingWildcard) {
                    pattern = "*" + pattern; //$NON-NLS-1$
                }
                matcher = new StringMatcher(pattern, true, false);
            }
        }

        /**
         * Clears the caches used for optimizing this filter. Needs to be called whenever the tree content changes.
         */
        /* package */void clearCaches() {
            cache.clear();
            foundAnyCache.clear();
        }

        /**
         * Answers whether the given String matches the pattern.
         * 
         * @param string the String to test
         * 
         * @return whether the string matches the pattern
         */
        private boolean match(String string) {
            if (matcher == null) {
                return true;
            }
            return matcher.match(string);
        }

        /**
         * Answers whether the given element is a valid selection in the filtered tree. For example, if a tree has items
         * that are categorized, the category itself may not be a valid selection since it is used merely to organize
         * the elements.
         * 
         * @param element
         * @return true if this element is eligible for automatic selection
         */
        public boolean isElementSelectable(Object element) {
            return element != null;
        }

        /**
         * Answers whether the given element in the given viewer matches the filter pattern. This is a default
         * implementation that will show a leaf element in the tree based on whether the provided filter text matches
         * the text of the given element's text, or that of it's children (if the element has any).
         * 
         * Subclasses may override this method.
         * 
         * @param viewer the tree viewer in which the element resides
         * @param element the element in the tree to check for a match
         * 
         * @return true if the element matches the filter pattern
         */
        public boolean isElementVisible(Viewer viewer, Object element) {
            return isParentMatch(viewer, element) || isLeafMatch(viewer, element);
        }

        /**
         * Check if the parent (category) is a match to the filter text. The default behavior returns true if the
         * element has at least one child element that is a match with the filter text.
         * 
         * Subclasses may override this method.
         * 
         * @param viewer the viewer that contains the element
         * @param element the tree element to check
         * @return true if the given element has children that matches the filter text
         */
        protected boolean isParentMatch(Viewer viewer, Object element) {
            Object[] children = ((ITreeContentProvider) ((AbstractTreeViewer) viewer).getContentProvider()).getChildren(element);

            if ((children != null) && (children.length > 0)) {
                return isAnyVisible(viewer, element, children);
            }
            return false;
        }

        /**
         * Check if the current (leaf) element is a match with the filter text. The default behavior checks that the
         * label of the element is a match.
         * 
         * Subclasses should override this method.
         * 
         * @param viewer the viewer that contains the element
         * @param element the tree element to check
         * @return true if the given element's label matches the filter text
         */
        protected boolean isLeafMatch(Viewer viewer, Object element) {
            String labelText = ((ILabelProvider) ((StructuredViewer) viewer).getLabelProvider()).getText(element);

            if (labelText == null) {
                return false;
            }
            return wordMatches(labelText);
        }

        /**
         * Return whether or not if any of the words in text satisfy the match critera.
         * 
         * @param text the text to match
         * @return boolean <code>true</code> if one of the words in text satisifes the match criteria.
         */
        protected boolean wordMatches(String text) {
            if (text == null) {
                return false;
            }

            // If the whole text matches we are all set
            if (match(text)) {
                return true;
            }

            // Otherwise check if any of the words of the text matches
            String[] words = text.split("\\W");//$NON-NLS-1$
            for (int i = 0; i < words.length; i++) {
                String word = words[i];
                if (match(word)) {
                    return true;
                }
            }

            return false;
        }

        /**
         * Can be called by the filtered tree to turn on caching.
         * 
         * @param useCache The useCache to set.
         */
        void setUseCache(boolean useCache) {
            this.useCache = useCache;
        }
    }

}
