/******************************************************************************
 * Product: Posterita Ajax UI 												  *
 * Copyright (C) 2007 Posterita Ltd.  All Rights Reserved.                    *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * Posterita Ltd., 3, Draper Avenue, Quatre Bornes, Mauritius                 *
 * or via info@posterita.org or http://www.posterita.org/                     *
 *****************************************************************************/
/******************************************************************************
 * Product: JPiere                                                            *
 * Copyright (C) Hideaki Hagiwara (h.hagiwara@oss-erp.co.jp)                  *
 *                                                                            *
 * This program is free software, you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY.                          *
 * See the GNU General Public License for more details.                       *
 *                                                                            *
 * JPiere is maintained by OSS ERP Solutions Co., Ltd.                        *
 * (http://www.oss-erp.co.jp)                                                 *
 *****************************************************************************/

package custom.window.webui.adwindow;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

import org.adempiere.util.Callback;
import org.adempiere.webui.adwindow.ADTabpanel;				//JPIERE
import org.adempiere.webui.adwindow.AbstractADWindowContent;	//JPIERE
import org.adempiere.webui.adwindow.BreadCrumb;
import org.adempiere.webui.adwindow.BreadCrumbLink;			//JPIERE
import org.adempiere.webui.adwindow.DetailPane;				//JPIERE
import org.adempiere.webui.adwindow.IADTabpanel;				//JPIERE
import org.adempiere.webui.component.ADTabListModel;
import org.adempiere.webui.component.ADTabListModel.ADTabLabel;
import org.adempiere.webui.util.ZKUpdateUtil;
import org.adempiere.webui.window.Dialog;
import org.compiere.model.DataStatusEvent;
import org.compiere.model.DataStatusListener;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.model.MTab;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.compiere.util.Evaluator;
import org.compiere.util.Msg;
import org.zkoss.zk.au.out.AuScript;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Execution;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.HtmlBasedComponent;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Center;
import org.zkoss.zul.Label;
import org.zkoss.zul.Menuitem;
import org.zkoss.zul.Row;
import org.zkoss.zul.RowRenderer;
import org.zkoss.zul.Vlayout;

/**
 * Header and detail UI for AD_Tabs.
 * This class manage a list of tabs with the current selected tab as the visible {@link ADTabpanel} instance.
 * Child tabs of selected tab is shown in {@link DetailPane} using {@link Tabbox}.
 *
 * @author  <a href="mailto:agramdass@gmail.com">Ashley G Ramdass</a>
 * @author <a href="mailto:hengsin@gmail.com">Low Heng Sin</a>
 * @date    Feb 25, 2007
 * @version $Revision: 0.10 $
 *
 * @author Hideaki Hagiwara（h.hagiwara@oss-erp.co.jp）
 *
 */
public class CustomCompositeADTabbox extends CustomAbstractADTabbox
{
	/**
	 * DetailPane attribute to hold list of child tabs.
	 * List of Object[] of tabIndex, tabPanel, tabLabel, enable.
	 */
	private static final String DETAILPANE_TABLIST_ATTR = "detailpane.tablist";

	/** Execution attribute to hold reference to detail ADTabpanel that's handling onEditDetail event **/
	public static final String AD_TABBOX_ON_EDIT_DETAIL_ATTRIBUTE = "ADTabbox.onEditDetail";

	/** after tab selection change event **/
	private static final String ON_POST_TAB_SELECTION_CHANGED_EVENT = "onPostTabSelectionChanged";

	/** event echo from ON_POST_TAB_SELECTION_CHANGED_EVENT handler **/
	private static final String ON_TAB_SELECTION_CHANGED_ECHO_EVENT = "onTabSelectionChangedEcho";

	/** tab selection change event **/
	public static final String ON_SELECTION_CHANGED_EVENT = "onSelectionChanged";

	/** List of all tab **/
    private List<ADTabListModel.ADTabLabel> tabLabelList = new ArrayList<ADTabListModel.ADTabLabel>();

	/** List of all tab panel **/
    private List<CustomIADTabpanel> tabPanelList = new ArrayList<CustomIADTabpanel>();

 	/** main layout component **/
    private Vlayout layout;
    
	/** tab selection change listener **/
	private EventListener<Event> selectionListener;

	/** {@link CustomIADTabpanel} instance for selected tab **/
	private CustomIADTabpanel headerTab;

	/** Index of selected tab **/
	private int selectedIndex = 0;

	/**
	 * default constructor
	 */
    public CustomCompositeADTabbox(){
    }

    /**
     * Create detail panel at bottom
     * @return {@link DetailPane}
     */
    protected CustomDetailPane createDetailPane() {
    	CustomDetailPane detailPane = new CustomDetailPane();
    	detailPane.setEventListener(new EventListener<Event>() {

			@Override
			public void onEvent(Event event) throws Exception {
				if (DetailPane.ON_EDIT_EVENT.equals(event.getName())) {
					if (headerTab.getGridTab().isNew() && ! headerTab.needSave(true, false)) return;

					final int row = getSelectedDetailADTabpanel() != null
							? getSelectedDetailADTabpanel().getGridTab().getCurrentRow()
							: 0;
					final boolean formView = event.getData() != null ? (Boolean)event.getData() : true;
					if (getSelectedDetailADTabpanel() != null &&
						((getSelectedDetailADTabpanel() == getDirtyADTabpanel()) ||
						(getDirtyADTabpanel() == null && getSelectedDetailADTabpanel().getGridTab().isNew()))) {
						onEditDetail(row, formView);
					} else {
						adWindowPanel.saveAndNavigate(new Callback<Boolean>() {
							@Override
							public void onCallback(Boolean result) {
								if (result)
									onEditDetail(row, formView);
							}
						});
					}
				}
				else if (DetailPane.ON_NEW_EVENT.equals(event.getName())) {
					if (headerTab.getGridTab().isNew()) return;

					final int row = getSelectedDetailADTabpanel() != null
							? getSelectedDetailADTabpanel().getGridTab().getCurrentRow()
							: 0;
					adWindowPanel.saveAndNavigate(new Callback<Boolean>() {
						@Override
						public void onCallback(Boolean result) {
							if (result) {
								if (getSelectedDetailADTabpanel().getGridTab().isSingleRow()) {
									if (headerTab.isDetailVisible() && headerTab.getDetailPane().getSelectedPanel().isToggleToFormView()) {
										if (!getSelectedDetailADTabpanel().getGridTab().isNew()) {
											getSelectedDetailADTabpanel().getGridTab().dataNew(false);
											getSelectedDetailADTabpanel().dynamicDisplay(0);
											focusToTabpanel(getSelectedDetailADTabpanel());
										}
									} else {
										onEditDetail(row, true);
										if (!adWindowPanel.getActiveGridTab().isNew())
											adWindowPanel.onNew();
									}
								} else {
									if (!getSelectedDetailADTabpanel().getGridTab().isNew()) {
										getSelectedDetailADTabpanel().getGridTab().dataNew(false);
										if (!headerTab.isDetailVisible()) {
											String uuid = headerTab.getCustomDetailPane().getParent().getUuid();
											String vid = getSelectedDetailADTabpanel().getCustomGridView().getUuid();
											String script = "setTimeout(function(){zk('#"+uuid+"').$().setOpen(true);setTimeout(function(){let v=zk('#" + vid
													+ "').$();let e=new zk.Event(v,'onEditCurrentRow',null,{toServer:true});zAu.send(e);},200);},200)";
											Clients.response(new AuScript(script));
										} else {
											boolean isFormView = headerTab.getCustomDetailPane().getSelectedPanel().isToggleToFormView();
											if (isFormView) {
												getSelectedDetailADTabpanel().dynamicDisplay(0);
												focusToTabpanel(getSelectedDetailADTabpanel());
											} else {
												getSelectedDetailADTabpanel().getCustomGridView().onEditCurrentRow();
											}
										}
									}
								}
							}
						}
					});
				}
				else if (DetailPane.ON_SAVE_EVENT.equals(event.getName())) {
					if (headerTab.getGridTab().isNew()) return;

					final IADTabpanel tabPanel = getSelectedDetailADTabpanel();
					if (!tabPanel.getGridTab().dataSave(true)) {
						showLastError();
					} else {
						tabPanel.getGridTab().dataRefreshAll(true, true);
						tabPanel.getGridTab().refreshParentTabs(true);
					}
				}
				else if (DetailPane.ON_DELETE_EVENT.equals(event.getName())) {
					onDelete();
				}
				else if (DetailPane.ON_QUICK_FORM_EVENT.equals(event.getName()))
				{
					if (headerTab.getGridTab().isNew() && !headerTab.needSave(true, false))
						return;

					final int row = getSelectedDetailADTabpanel() != null ? getSelectedDetailADTabpanel().getGridTab().getCurrentRow() : 0;
					final boolean formView = event.getData() != null ? (Boolean) event.getData() : true;

					adWindowPanel.saveAndNavigate(new Callback <Boolean>() {
						@Override
						public void onCallback(Boolean result)
						{
							if (result)
							{
								onEditDetail(row, formView);
								adWindowPanel.onQuickForm(true);
							}
						}
					});
				}
				else if (DetailPane.ON_RECORD_NAVIGATE_EVENT.equals(event.getName())) {
					final String action = (String) event.getData();
					adWindowPanel.saveAndNavigate(new Callback <Boolean>() {
						@Override
						public void onCallback(Boolean result)
						{
							if (result)
							{
								if ("first".equalsIgnoreCase(action)) {
									getSelectedDetailADTabpanel().getGridTab().navigate(0);
								} else if ("previous".equalsIgnoreCase(action)) {
									getSelectedDetailADTabpanel().getGridTab().navigateRelative(-1);
								} else if ("next".equalsIgnoreCase(action)) {
									getSelectedDetailADTabpanel().getGridTab().navigateRelative(1);
								} else if ("last".equalsIgnoreCase(action)) {
									getSelectedDetailADTabpanel().getGridTab().navigate(getSelectedDetailADTabpanel().getGridTab().getRowCount()-1);
								}
							}
						}
					});
				}
			}

			/**
			 * Delete current row of selected detail tab
			 */
			private void onDelete() {
				if (headerTab.getGridTab().isNew()) return;

				final CustomIADTabpanel tabPanel = getSelectedDetailADTabpanel();
				if (tabPanel != null && tabPanel.getGridTab().getSelection().length > 0) {
					onDeleteSelected(tabPanel);
				}
				else if (tabPanel != null && tabPanel.getGridTab().getRowCount() > 0
					&& tabPanel.getGridTab().getCurrentRow() >= 0) {
					Dialog.ask(tabPanel.getGridTab().getWindowNo(), "DeleteRecord?", new Callback<Boolean>() {

						@Override
						public void onCallback(Boolean result) {
							if (!result) return;
							if (!tabPanel.getGridTab().dataDelete()) {
								showLastError();
							} else {
								adWindowPanel.onRefresh(true);
							}
						}
					});
				}
			}

			/**
			 * Delete selected rows of selected detail tab
			 * @param tabPanel
			 */
			private void onDeleteSelected(final CustomIADTabpanel tabPanel) {
				if (tabPanel == null || tabPanel.getGridTab() == null) return;

				final int[] indices = tabPanel.getGridTab().getSelection();
				if(indices.length > 0) {
					StringBuilder sb = new StringBuilder();
					sb.append(Env.getContext(Env.getCtx(), tabPanel.getGridTab().getWindowNo(), "_WinInfo_WindowName", false)).append(" - ")
						.append(indices.length).append(" ").append(Msg.getMsg(Env.getCtx(), "Selected"));
					Dialog.ask(sb.toString(), tabPanel.getGridTab().getWindowNo(),"DeleteSelection", new Callback<Boolean>() {
						@Override
						public void onCallback(Boolean result) {
							if(result){
								tabPanel.getGridTab().clearSelection();
								Arrays.sort(indices);
								int offset = 0;
								int count = 0;
								for (int i = 0; i < indices.length; i++)
								{
									tabPanel.getGridTab().navigate(indices[i]-offset);
									if (tabPanel.getGridTab().dataDelete())
									{
										offset++;
										count++;
									}
								}

								adWindowPanel.onRefresh(true);
								adWindowPanel.getStatusBar().setStatusLine(Msg.getMsg(Env.getCtx(), "Deleted")+": "+count, false);
							}
						}
					});
				}
			}
		});

    	return detailPane;
    }

    /**
     * defer execution of adTabPanel.focus()
     * @param adTabPanel
     */
    private void focusToTabpanel(CustomIADTabpanel adTabPanel ) {
		if (adTabPanel != null && adTabPanel instanceof HtmlBasedComponent) {
			final HtmlBasedComponent comp = (HtmlBasedComponent) adTabPanel;
			Executions.schedule(layout.getDesktop(), e -> {comp.focus();}, new Event("onFocusDefer"));
		}
	}

    /**
     * Edit current row of selected detail tab.
     * Make selected detail tab the new header tab.
     * @param row
     * @param formView
     */
    protected void onEditDetail(int row, boolean formView) {

		int oldIndex = selectedIndex;
		CustomIADTabpanel selectedPanel = getSelectedDetailADTabpanel();
		if (selectedPanel == null) return;
		int newIndex = selectedPanel.getTabNo();
		selectedPanel.query();

		Executions.getCurrent().setAttribute(AD_TABBOX_ON_EDIT_DETAIL_ATTRIBUTE, selectedPanel);
		Event selectionChanged = new Event(ON_SELECTION_CHANGED_EVENT, layout, new Object[]{oldIndex, newIndex});
		try {
			selectionListener.onEvent(selectionChanged);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		headerTab.setDetailPaneMode(false);
		if (formView && headerTab.isGridView()) {
			headerTab.switchRowPresentation();
		}

		if (!headerTab.getGridTab().isSortTab() && headerTab instanceof CustomADTabpanel)
			headerTab.getGridTab().setCurrentRow(row, true);

		if (headerTab.isGridView()) {
			if (headerTab.getGridTab().isNew() || headerTab.needSave(true, false)) {
				headerTab.getCustomGridView().onEditCurrentRow();
			}
		} else {
			((HtmlBasedComponent)headerTab).focus();
		}
	}

    /**
     * Create layout and setup listeners for bread crumb.
     * Vertical layout with {@link ADTabpanel} as the only child component.
     */
    @Override
    protected Component doCreatePart(Component parent)
    {
    	layout = new Vlayout();
    	ZKUpdateUtil.setHeight(layout, "100%");
    	ZKUpdateUtil.setWidth(layout, "100%");
    	layout.setStyle("position: relative");
    	if (parent != null) {
    		layout.setParent(parent);
    	} else {
    		layout.setPage(page);
    	}

    	layout.addEventListener(ON_POST_TAB_SELECTION_CHANGED_EVENT, new EventListener<Event>() {
			@Override
			public void onEvent(Event event) throws Exception {
				onPostTabSelectionChanged((Boolean)event.getData());
			}
		});

    	layout.addEventListener(ON_TAB_SELECTION_CHANGED_ECHO_EVENT, new EventListener<Event>() {
			@Override
			public void onEvent(Event event) throws Exception {
				onTabSelectionChangedEcho((Boolean)event.getData());
			}
		});

    	CustomBreadCrumb breadCrumb = getBreadCrumb();
    	breadCrumb.addEventListener(Events.ON_CLICK, new EventListener<Event>() {
			@Override
			public void onEvent(Event event) throws Exception {
				//send tab selection change event
				int oldIndex = selectedIndex;
				if (event.getTarget() instanceof BreadCrumbLink) {
					BreadCrumbLink link = (BreadCrumbLink) event.getTarget();
					int newIndex = Integer.parseInt(link.getPathId());

					Event selectionChanged = new Event(ON_SELECTION_CHANGED_EVENT, layout, new Object[]{oldIndex, newIndex});
					selectionListener.onEvent(selectionChanged);
				} else if (event.getTarget() instanceof Menuitem) {
					Menuitem item = (Menuitem) event.getTarget();
					int newIndex = Integer.parseInt(item.getValue());

					Event selectionChanged = new Event(ON_SELECTION_CHANGED_EVENT, layout, new Object[]{oldIndex, newIndex});
					selectionListener.onEvent(selectionChanged);
				}
			}
		});

    	return layout;
    }

    @Override
	protected void doAddTab(GridTab gTab, CustomIADTabpanel tabPanel) {
    	ADTabListModel.ADTabLabel tabLabel = new ADTabListModel.ADTabLabel(gTab.getName(), gTab.getTabLevel(),gTab.getDescription(),
        		gTab.getWindowNo(),gTab.getAD_Tab_ID());
        tabLabelList.add(tabLabel);
        tabPanelList.add(tabPanel);

        tabPanel.setTabNo(tabPanelList.size()-1);

        tabPanel.addEventListener(ADTabpanel.ON_ACTIVATE_EVENT, new EventListener<Event>() {
			@Override
			public void onEvent(Event event) throws Exception {
				Boolean b = (Boolean) event.getData();
				if (b != null && !b.booleanValue())
					return;

				CustomIADTabpanel tabPanel = (CustomIADTabpanel) event.getTarget();
				//call onActivateDetail if it is detail tab panel
				if (tabPanel != headerTab && headerTab.getCustomDetailPane() != null && tabPanel.getTabLevel() > headerTab.getTabLevel()) {
					if (b != null && b.booleanValue()) {
						onActivateDetail(tabPanel);
						if (headerTab instanceof CustomADTabpanel) {
							if (!((CustomADTabpanel) headerTab).getADWindowContent().focusToLastFocusEditor(true))
								((CustomADTabpanel) headerTab).getADWindowContent().focusToActivePanel();
						}
					}
				}
			}
		});

        tabPanel.addEventListener(DetailPane.ON_ACTIVATE_DETAIL_EVENT, new EventListener<Event>() {
			@Override
			public void onEvent(Event event) throws Exception {
				final CustomIADTabpanel tabPanel = (CustomIADTabpanel) event.getTarget();
				int oldIndex = (Integer) event.getData();
				if (oldIndex != headerTab.getCustomDetailPane().getSelectedIndex()) {
					CustomIADTabpanel prevTabPanel = headerTab.getCustomDetailPane().getADTabpanel(oldIndex);
					if (prevTabPanel != null && prevTabPanel.needSave(true, true)) {
						final int newIndex = headerTab.getCustomDetailPane().getSelectedIndex();
						headerTab.getCustomDetailPane().setSelectedIndex(oldIndex);
						adWindowPanel.saveAndNavigate(new Callback<Boolean>() {
							@Override
							public void onCallback(Boolean result) {
								if (result) {
									headerTab.getCustomDetailPane().setSelectedIndex(newIndex);
									tabPanel.activate(true);
								}
							}
						});
					} else {
						headerTab.getCustomDetailPane().setSelectedIndex(headerTab.getCustomDetailPane().getSelectedIndex());
						tabPanel.activate(true);
					}
				} else {
					tabPanel.activate(true);
				}
			}
        });

        tabPanel.addEventListener(ADTabpanel.ON_SWITCH_VIEW_EVENT, new EventListener<Event>() {

			@Override
			public void onEvent(Event event) throws Exception {
				CustomIADTabpanel tabPanel = (CustomIADTabpanel) event.getTarget();
				if (tabPanel == headerTab) {
					CustomIADTabpanel detailPanel = getSelectedDetailADTabpanel();
					if (detailPanel != null) {
						detailPanel.setDetailPaneMode(true);
					}
					if (headerTab.getCustomDetailPane() != null)
						headerTab.getCustomDetailPane().setVflex("true");
				}
			}
		});

        tabPanel.addEventListener(ADTabpanel.ON_TOGGLE_EVENT, new EventListener<Event>() {

			@Override
			public void onEvent(Event event) throws Exception {
				CustomIADTabpanel tabPanel = (CustomIADTabpanel) event.getTarget();
				if (tabPanel == headerTab) {
					adWindowPanel.onToggle();
				} else {
					headerTab.getCustomDetailPane().onEdit(true);
				}

			}
		});

        if (tabPanel.getCustomGridView() != null) {
	        tabPanel.getCustomGridView().addEventListener(DetailPane.ON_EDIT_EVENT, new EventListener<Event>() {
				@Override
				public void onEvent(Event event) throws Exception {
					CustomGridView gridView = (CustomGridView) event.getTarget();
					if (!gridView.isDetailPaneMode()) {
						adWindowPanel.onToggle();
					}
				}
			});
        }

        //add to header or detail pane
    	if (layout.getChildren().isEmpty()) {
    		layout.appendChild(tabPanel);
    		headerTab = tabPanel;
    	} else if (tabLabel.tabLevel == 1) {
    		if (headerTab.getCustomDetailPane() == null) {
    			headerTab.setCustomDetailPane(createDetailPane());
    		} else
    			tabPanel.setVisible(false);
    		ZKUpdateUtil.setHflex(headerTab.getCustomDetailPane(), "1");
    		headerTab.getCustomDetailPane().addADTabpanel(tabPanel, tabLabel);
    		tabPanel.setDetailPaneMode(true);
    		headerTab.getCustomDetailPane().setVflex("true");
    	} else if (tabLabel.tabLevel > 1){
    		headerTab.getCustomDetailPane().addADTabpanel(tabPanel, tabLabel, false);
    		tabPanel.setDetailPaneMode(true);
    		headerTab.getCustomDetailPane().setVflex("true");
    	}
    	HtmlBasedComponent htmlComponent = (HtmlBasedComponent) tabPanel;
    	ZKUpdateUtil.setVflex(htmlComponent, "1");
    	ZKUpdateUtil.setWidth(htmlComponent, "100%");

        tabPanel.getGridTab().addDataStatusListener(new SyncDataStatusListener(tabPanel));
	}

	@Override
	public boolean updateSelectedIndex(int oldIndex, int newIndex) {
		boolean b = super.updateSelectedIndex(oldIndex, newIndex);
		if (b) {
			CustomBreadCrumb breadcrumb = getBreadCrumb();
			if (breadcrumb.isEmpty()) {
				updateBreadCrumb();
			}
		}
		return b;
	}

	/**
	 * Call {@link CustomADTabpanel#activateDetailIfVisible()}
	 */
	private void activateDetailIfVisible() {
    	if (headerTab instanceof CustomADTabpanel) {
	    	((CustomADTabpanel)headerTab).activateDetailIfVisible();
    	}
	}

    @Override
	protected void updateTabState() {
    	if (isDetailPaneLoaded())
    	{
    		boolean hasChanges = false;
    		for(int i = 0; i < headerTab.getCustomDetailPane().getTabcount(); i++)
    		{
    			CustomIADTabpanel adtab = headerTab.getCustomDetailPane().getADTabpanel(i);
    			if (adtab.getDisplayLogic() != null && adtab.getDisplayLogic().trim().length() > 0) {
    				boolean visible = Evaluator.evaluateLogic(headerTab, adtab.getDisplayLogic());
    				if (headerTab.getCustomDetailPane().isTabVisible(i) != visible) {
    					headerTab.getCustomDetailPane().setTabVisibility(i, visible);
    					hasChanges = true;
    				}
    			}
    		}
    		int selected = headerTab.getCustomDetailPane().getSelectedIndex();
    		if (headerTab.getCustomDetailPane().getADTabpanel(selected) == null || !headerTab.getCustomDetailPane().isTabVisible(selected)) {
    			for(int i = 0; i < headerTab.getCustomDetailPane().getTabcount(); i++) {
    				if (selected == i) continue;
    				if (headerTab.getCustomDetailPane().isTabVisible(i)) {
    					headerTab.getCustomDetailPane().setSelectedIndex(i);
    					if (headerTab instanceof CustomADTabpanel) {
    						((CustomADTabpanel) headerTab).activateDetailIfVisible();
    					}
    					break;
    				}
    			}
    			hasChanges = true;
    		}
    		if (hasChanges) {
    			if (headerTab.getCustomDetailPane().getParent() != null)
    				headerTab.getCustomDetailPane().getParent().invalidate();
    		}
    	}
	}

    /**
     * Return the selected Tab Panel
     */
     @Override
    public CustomIADTabpanel getSelectedTabpanel()
    {
        return tabPanelList.isEmpty() ? null : tabPanelList.get(selectedIndex);
    }

    @Override
    public int getSelectedIndex() {
    	return selectedIndex;
    }

    @Override
	public void setSelectionEventListener(EventListener<Event> listener) {
		selectionListener = listener;
	}

	@Override
	protected void doTabSelectionChanged(int oldIndex, int newIndex) {
		selectedIndex = newIndex;
		CustomIADTabpanel oldTabpanel = oldIndex >= 0 ? tabPanelList.get(oldIndex) : null;
        CustomIADTabpanel newTabpanel = tabPanelList.get(newIndex);
        if (oldTabpanel != null) {
        	oldTabpanel.setVisible(false);
        }
        newTabpanel.createUI();
        newTabpanel.setVisible(true);

        headerTab = newTabpanel;
        if (headerTab.getParent() != layout)
			layout.appendChild(headerTab);

		//set state
		headerTab.setDetailPaneMode(false);
		//show empty path, update later with actual path in onPostTabSelectionChanged
		getBreadCrumb().getFirstChild().getChildren().clear();
		getBreadCrumb().getFirstChild().appendChild(new Label(""));

		Events.sendEvent(new Event(ON_POST_TAB_SELECTION_CHANGED_EVENT, layout, oldIndex > newIndex));
	}

	/**
	 * Handle after tab selection change event, echo onTabSelectionChangedEcho event.
	 * @param back
	 */
	private void onPostTabSelectionChanged(Boolean back) {
		if (headerTab instanceof CustomADTabpanel && !headerTab.getGridTab().isSortTab()) {
			//gather all child tabs (both immediate and not immediate)
			//Object[]: tabIndex, tabPanel, tabLabel, enable
			List<Object[]> list = new ArrayList<Object[]>();
			int tabIndex = -1;
			int currentLevel = headerTab.getTabLevel();
			for (int i = selectedIndex + 1; i< tabPanelList.size(); i++) {
				CustomIADTabpanel tabPanel = tabPanelList.get(i);
				int tabLevel = tabPanel.getTabLevel();
				ADTabListModel.ADTabLabel tabLabel = tabLabelList.get(i);
				if ((tabLevel - currentLevel) == 1) {
					tabIndex++;
					Object[] value = new Object[]{tabIndex, tabPanel, tabLabel, Boolean.TRUE};
					list.add(value);
				} else if (tabLevel > currentLevel ){
					tabIndex++;
					Object[] value = new Object[]{tabIndex, tabPanel, tabLabel, Boolean.FALSE};
					list.add(value);
		    	} else {
		    		break;
		    	}
			}

			if (!list.isEmpty()) {
				CustomDetailPane detailPane = headerTab.getCustomDetailPane();
				if (detailPane == null) {
					detailPane = createDetailPane();
				}
				detailPane.setAttribute(DETAILPANE_TABLIST_ATTR, list);

				ZKUpdateUtil.setVflex(detailPane, "true");
				if (headerTab.getCustomDetailPane() == null) {
					headerTab.setCustomDetailPane(detailPane);
				}
			}
		}
		
		updateBreadCrumb();
		
		Events.echoEvent(new Event(ON_TAB_SELECTION_CHANGED_ECHO_EVENT, layout, back));
	}

	/**
	 * final UI update event for tab selection change
	 * @param back
	 */
	private void onTabSelectionChangedEcho(Boolean back) {
		if (headerTab instanceof CustomADTabpanel) {
			CustomDetailPane detailPane = headerTab.getCustomDetailPane();

			//setup tabs of detail pane
			if (detailPane != null) {
				@SuppressWarnings("unchecked")
				//tabIndex, tabPanel, tabLabel, enable
				List<Object[]> list = (List<Object[]>) detailPane.removeAttribute(DETAILPANE_TABLIST_ATTR);
				if (list != null && !list.isEmpty()) {
					int currentLevel = headerTab.getTabLevel();
					for (Object[] value : list) {
						int tabIndex = (Integer) value[0];
						CustomIADTabpanel tabPanel = (CustomIADTabpanel) value[1];
						ADTabLabel tabLabel = (ADTabLabel) value[2] ;
						Boolean enable = (Boolean) value[3];

						int tabLevel = tabPanel.getTabLevel();
						if ((tabLevel - currentLevel) == 1 || (tabLevel == 0 && currentLevel == 0)) {
							if (tabPanel.isActivated() && !tabPanel.isGridView()) {
				    			tabPanel.switchRowPresentation();
				    		}
							if (tabPanel.getParent() != null) 
								tabPanel.setVisible(false);
						}
						tabPanel.setDetailPaneMode(true);
						detailPane.setADTabpanel(tabIndex, tabPanel, tabLabel, enable.booleanValue());
					}
					if (back == null || !back.booleanValue()) {
						detailPane.setSelectedIndex(0);
						activateDetailIfVisible();
					} else {
						if (headerTab.isDetailVisible() && detailPane.getSelectedADTabpanel() != null) {
							CustomIADTabpanel selectDetailPanel = detailPane.getSelectedADTabpanel();
							if (!selectDetailPanel.isVisible()) {
								selectDetailPanel.setVisible(true);
							}
							if (!selectDetailPanel.isGridView()) {
								boolean switchToGrid = true;
								Component parent = selectDetailPanel.getParent();
								while (parent != null) {
									if (parent instanceof CustomDetailPane.Tabpanel) {
										CustomDetailPane.Tabpanel dtp = (CustomDetailPane.Tabpanel) parent;
										switchToGrid = !dtp.isToggleToFormView();
										dtp.afterToggle();
										break;
									}
									parent = parent.getParent();
								}
								if (switchToGrid)
									selectDetailPanel.switchRowPresentation();
							}
							if (selectDetailPanel instanceof CustomADTabpanel)
							{
								((CustomADTabpanel)selectDetailPanel).activated = true;
								String msg = ((CustomADTabpanel)selectDetailPanel).getGridTab().getRowCount() + " " + Msg.getMsg(Env.getCtx(), "Records");
								setDetailPaneStatusMessage(msg, false);
							}

							if (selectDetailPanel.getGridTab().isTreeTab() && selectDetailPanel.getTreePanel() != null) {
								if (selectDetailPanel.getGridTab().getTreeDisplayedOn().equals(MTab.TREEDISPLAYEDON_MasterTab))
									selectDetailPanel.getCustomTreePanel().getParent().setVisible(false);
								else
									selectDetailPanel.getCustomTreePanel().getParent().setVisible(true);
							}
						}
					}
				}
			}
		}

        updateTabState();

        CustomADWindow adwindow = CustomADWindow.findADWindow(layout);
        if (adwindow != null) {
        	adwindow.getADWindowContent().getToolbar().enableTabNavigation(getBreadCrumb().hasParentLink(),
        			headerTab.getCustomDetailPane() != null && headerTab.getCustomDetailPane().getTabcount() > 0);
        }

        //indicator and row highlight lost after navigate back from child to parent
        if (back != null && back.booleanValue()) {
        	if (headerTab.isGridView()) {
        		RowRenderer<Object[]> renderer = headerTab.getCustomGridView().getListbox().getRowRenderer();
        		CustomGridTabRowRenderer gtr = (CustomGridTabRowRenderer)renderer;
        		Row row = gtr.getCurrentRow();
        		if (row != null)
        			gtr.setCurrentRow(row);
        	}
        }
	}

	/**
	 * update breadcrumb path
	 */
	private void updateBreadCrumb() {
		CustomBreadCrumb breadCrumb = getBreadCrumb();
		breadCrumb.reset();
		//add parent path
		if (selectedIndex > 0) {
			List<ADTabLabel> parents = new ArrayList<ADTabListModel.ADTabLabel>();
			List<Integer> parentIndex = new ArrayList<Integer>();
			int currentLevel = headerTab.getTabLevel();
			for(int i = selectedIndex - 1; i >= 0; i--) {
				ADTabLabel tabLabel = tabLabelList.get(i);
				if (tabLabel.tabLevel == currentLevel-1) {
					parents.add(tabLabel);
					parentIndex.add(i);
					currentLevel = tabLabel.tabLevel;
				}
			}
			Collections.reverse(parents);
			Collections.reverse(parentIndex);
			for(ADTabLabel tabLabel : parents) {
				int index = parentIndex.remove(0);
				breadCrumb.addPath(tabLabel.label, Integer.toString(index), true);
			}
		}
		ADTabLabel tabLabel = tabLabelList.get(selectedIndex);
		breadCrumb.addPath(tabLabel.label, Integer.toString(selectedIndex), false);
		if (!breadCrumb.isVisible())
			breadCrumb.setVisible(true);

		//Links for other child tabs at same level
		//Tab Index:Tab Label 
		LinkedHashMap<String, String> links = new LinkedHashMap<String, String>();
		int parentIndex = 0;
		if (headerTab.getTabLevel() > 1) {
			for(int i = selectedIndex - 1; i > 0; i--) {
				tabLabel = tabLabelList.get(i);
				if (tabLabel.tabLevel == (headerTab.getTabLevel()-1)) {
					parentIndex = i;
					break;
				}
			}
		}
		if (headerTab.getTabLevel() == 0)
		{
			for(int i = 0; i < tabLabelList.size(); i++) {
				if (i == selectedIndex) continue;
				tabLabel = tabLabelList.get(i);
				if (tabLabel.tabLevel == headerTab.getTabLevel()) {
					CustomIADTabpanel adtab = tabPanelList.get(i);
	    			if (adtab.getDisplayLogic() != null && adtab.getDisplayLogic().trim().length() > 0) {
	    				if (!Evaluator.evaluateLogic(headerTab, adtab.getDisplayLogic())) {
	    					continue;
	    				}
	    			}
					links.put(Integer.toString(i), tabLabel.label);
				}
			}
		}
		else
		{
			for(int i = parentIndex+1; i < tabLabelList.size(); i++) {
				if (i == selectedIndex) continue;

				tabLabel = tabLabelList.get(i);
				if (tabLabel.tabLevel == headerTab.getTabLevel()) {
					CustomIADTabpanel adtab = tabPanelList.get(i);
	    			if (adtab.getDisplayLogic() != null && adtab.getDisplayLogic().trim().length() > 0) {
	    				if (!Evaluator.evaluateLogic(headerTab, adtab.getDisplayLogic())) {
	    					continue;
	    				}
	    			}
					links.put(Integer.toString(i), tabLabel.label);
				} else if (tabLabel.tabLevel < headerTab.getTabLevel()) {
					break;
				}
			}
		}

		if (!links.isEmpty()) {
			breadCrumb.addLinks(links);
		}
	}

	/**
	 * @return {@link BreadCrumb}
	 */
	private CustomBreadCrumb getBreadCrumb() {
		CustomADWindowContent window = (CustomADWindowContent) adWindowPanel;
		CustomBreadCrumb breadCrumb = window.getBreadCrumb();
		return breadCrumb;
	}

	@Override
	public Component getComponent() {
		return layout;
	}

	@Override
	public CustomIADTabpanel findADTabpanel(GridTab gTab) {
		for (CustomIADTabpanel tabpanel : tabPanelList) {
			if (tabpanel.getGridTab() == gTab) {
				return tabpanel;
			}
		}
		return null;
	}

	/**
	 * Notify selected detail tab after data status change of header tab	 
	 */
	class SyncDataStatusListener implements DataStatusListener {

		private CustomIADTabpanel tabPanel;

		SyncDataStatusListener(CustomIADTabpanel tabpanel) {
			this.tabPanel = tabpanel;
		}

		@Override
		public void dataStatusChanged(DataStatusEvent e) {
			Execution execution = Executions.getCurrent();
			if (execution == null) return;

			if (tabPanel == headerTab && e.getChangedColumn() == -1
				&& isDetailActivated()) {
				ArrayList<String> parentColumnNames = new ArrayList<String>();
	        	GridField[] parentFields = headerTab.getGridTab().getFields();
	        	for (GridField parentField : parentFields) {
	        		parentColumnNames.add(parentField.getColumnName());
	        	}

	        	CustomIADTabpanel detailTab = getSelectedDetailADTabpanel();
	        	if (detailTab != null) {
		        	//check is data action from detail tab
	        		String uuid = (String) execution.getAttribute(CustomCompositeADTabbox.class.getName()+".dataAction");
	        		if (uuid != null && uuid.equals(detailTab.getUuid()) && detailTab.getGridTab().isCurrent()) {
	        			//refresh current row
	        			detailTab.getGridTab().dataRefresh(false);
	        			//keep focus
	        			Clients.scrollIntoView(detailTab);

	        			return;
	        		}

	        		GridTab tab = detailTab.getGridTab();
	        		GridField[] fields = tab.getFields();
	        		for (GridField field : fields)
	        		{
	        			if (!parentColumnNames.contains(field.getColumnName()))
	        				Env.setContext(Env.getCtx(), field.getWindowNo(), field.getColumnName(), "");
	        		}
	        		detailTab.activate(true);
	        		detailTab.setDetailPaneMode(true);
	        	}
	        	headerTab.getCustomDetailPane().setVflex("true");
			}
		}

	}

	@Override
	public void onDetailRecord() {
		if (headerTab.getCustomDetailPane() != null && getSelectedDetailADTabpanel() != null) {
			try {
				if (!getSelectedDetailADTabpanel().isActivated()) {
					onActivateDetail(getSelectedDetailADTabpanel());
				}
				headerTab.getCustomDetailPane().onEdit(getSelectedDetailADTabpanel().getGridTab().isSingleRow());
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}

	/**
	 * @return true if selected detail tab have been activated
	 */
	public boolean isDetailActivated() {
		if (headerTab instanceof CustomADTabpanel) {
			CustomADTabpanel atp = (CustomADTabpanel) headerTab;
			return atp.hasDetailTabs() && getSelectedDetailADTabpanel() != null &&
					getSelectedDetailADTabpanel().isActivated();
		}
		return false;
	}

	@Override
	public boolean isSortTab() {
		return headerTab != null ? headerTab.getGridTab().isSortTab() : false;
	}

	@Override
	public CustomIADTabpanel getSelectedDetailADTabpanel() {
		if (headerTab instanceof CustomADTabpanel && ((CustomADTabpanel)headerTab).hasDetailTabs()) {
			return headerTab.getCustomDetailPane().getSelectedADTabpanel();
		}
		return null;
	}

	@Override
	public boolean needSave(boolean rowChange, boolean onlyRealChange) {
		boolean b = headerTab.needSave(rowChange, onlyRealChange);
		if (b)
			return b;

		CustomIADTabpanel detailPanel = getSelectedDetailADTabpanel();
		if (detailPanel != null) {
			b = detailPanel.needSave(rowChange, onlyRealChange);
		}

		return b;
	}

	@Override
	public void dataIgnore() {
		CustomIADTabpanel detailPanel = getSelectedDetailADTabpanel();
		if (detailPanel != null) {
			if (detailPanel instanceof CustomADSortTab) {
				detailPanel.refresh();
				if (((CustomADSortTab) detailPanel).isChanged()) {
					((CustomADSortTab) detailPanel).setIsChanged(false);
				}
			} else {
				detailPanel.getGridTab().dataIgnore();
			}
		}
		headerTab.getGridTab().dataIgnore();
	}

	@Override
	public GridTab getSelectedGridTab() {
		CustomIADTabpanel tabpanel = getSelectedTabpanel();
		return tabpanel == null ? null : tabpanel.getGridTab();
	}

	@Override
	public boolean dataSave(boolean onSaveEvent) {
		CustomIADTabpanel detail = getSelectedDetailADTabpanel();
		if (detail != null && detail.needSave(true, true)) {
			Execution execution = Executions.getCurrent();
			if (execution != null) {
				execution.setAttribute(getClass().getName()+".dataAction", detail.getUuid());
			}
			return detail.dataSave(onSaveEvent);
		}
		return headerTab.dataSave(onSaveEvent);
	}

	@Override
	public void setDetailPaneStatusMessage(String status, boolean error) {
		headerTab.getCustomDetailPane().setStatusMessage(status, error);
	}

	@Override
	public CustomIADTabpanel getDirtyADTabpanel() {
		CustomIADTabpanel detail = getSelectedDetailADTabpanel();
		if (detail != null && detail.needSave(true, true)) {
			return detail;
		} else if (headerTab.needSave(true, true)) {
			return headerTab;
		}

		return null;
	}

	/**
	 * activate detail tab panel
	 * @param tabPanel
	 */
	private void onActivateDetail(CustomIADTabpanel tabPanel) {
		tabPanel.createUI();
		if (headerTab.getGridTab().isNew()) {
			tabPanel.resetDetailForNewParentRecord();
		} else {
			//maintain detail row position if possible
			int currentRow = -1;
			if (!tabPanel.getGridTab().isSortTab()) {
				currentRow = tabPanel.getGridTab().getCurrentRow();
			}
			tabPanel.query(false, 0, 0);
			if (currentRow >= 0 && currentRow != tabPanel.getGridTab().getCurrentRow()
				&& currentRow < tabPanel.getGridTab().getRowCount()) {
				tabPanel.getGridTab().setCurrentRow(currentRow, false);
			}
		}
		if (!tabPanel.isVisible()) {
			tabPanel.setVisible(true);
			if (tabPanel.getDesktop() != null) {
				Executions.schedule(tabPanel.getDesktop(), e -> {
					invalidateTabPanel(tabPanel);
				}, new Event("onPostActivateDetail", tabPanel));
			} else {
				invalidateTabPanel(tabPanel);
			}
		} else {
			invalidateTabPanel(tabPanel);
		}
		boolean wasForm = false;
		if (!tabPanel.isGridView()) {
			tabPanel.switchRowPresentation(); // required to avoid NPE on GridTabRowRenderer.getCurrentRow below
			wasForm = true;
		}
		tabPanel.setDetailPaneMode(true);
		headerTab.getCustomDetailPane().setVflex("true");
		if (tabPanel instanceof CustomADSortTab) {
			headerTab.getCustomDetailPane().updateToolbar(false, true);
		} else {
			tabPanel.dynamicDisplay(0);
			if (tabPanel.getCustomGridView() != null && tabPanel.getCustomGridView().getListbox() != null) {
				RowRenderer<Object[]> renderer = tabPanel.getCustomGridView().getListbox().getRowRenderer();
				if (renderer != null) {
					CustomGridTabRowRenderer gtr = (CustomGridTabRowRenderer)renderer;
					Row row = gtr.getCurrentRow();
					if (row != null)
						gtr.setCurrentRow(row);
				}
			}
		}
		if (wasForm) {
			// maintain form on header when zooming to a detail tab
			if (tabPanel.getTabLevel() == 0 && headerTab.getTabLevel() != 0) {
				tabPanel.switchRowPresentation();
			} else {
				Component parent = tabPanel.getParent();
				while (parent != null) {
					if (parent instanceof CustomDetailPane.Tabpanel) {
						CustomDetailPane.Tabpanel dtp = (CustomDetailPane.Tabpanel) parent;
						if (dtp.isToggleToFormView()) {
							tabPanel.switchRowPresentation();
							dtp.afterToggle();
						}
						break;
					}
					parent = parent.getParent();
				}
			}
		}
	}

	/**
	 * force invalidate of tabPanel
	 * @param tabPanel
	 */
	private void invalidateTabPanel(CustomIADTabpanel tabPanel) {
		Center center = findCenter(tabPanel.getCustomGridView());
		if (center != null)
			center.invalidate();
		else
			tabPanel.invalidate();
	}

	/**
	 * Find {@link Center} that own gridView
	 * @param gridView
	 * @return {@link Center}
	 */
	private Center findCenter(CustomGridView gridView) {
		if (gridView == null)
			return null;
		Component p = gridView.getParent();
		while (p != null) {
			if (p instanceof Center)
				return (Center)p;
			p = p.getParent();
		}
		return null;
	}

	/**
	 * show last error message from CLogger
	 */
	private void showLastError() {
		String msg = CLogger.retrieveErrorString(null);
		if (msg != null)
		{
			headerTab.getCustomDetailPane().setStatusMessage(Msg.getMsg(Env.getCtx(), msg), true);
		}
		//other error will be catch in the dataStatusChanged event
	}

	@Override
	public void updateDetailPaneToolbar(boolean changed, boolean readOnly) {
		if (headerTab.getGridTab().isNew() || headerTab.getGridTab().getRowCount() == 0)
			headerTab.getCustomDetailPane().disableToolbar();
		else
			headerTab.getCustomDetailPane().updateToolbar(changed, readOnly);
	}

	@Override
	public boolean isDetailPaneLoaded() {
		if (headerTab.getCustomDetailPane() == null || headerTab.getCustomDetailPane().getTabcount() == 0)
			return false;
		for(int i = 0; i < headerTab.getCustomDetailPane().getTabcount(); i++) {
			if (headerTab.getCustomDetailPane().getADTabpanel(i) == null)
				return false;
		}
		return true;
	}

	@Override
	public void setDetailPaneSelectedTab(int adTabNo, int currentRow) {
		if (headerTab instanceof CustomADTabpanel && ((CustomADTabpanel) headerTab).hasDetailTabs()) {
			for(int i = 0; i < headerTab.getCustomDetailPane().getTabcount(); i++) {
				CustomIADTabpanel adtab = headerTab.getCustomDetailPane().getADTabpanel(i);
				if (adtab == null) continue;
				int tabNo = adtab.getTabNo();
				if (tabNo == adTabNo) {
					if (!headerTab.getCustomDetailPane().isTabVisible(i) || !headerTab.getCustomDetailPane().isTabEnabled(i)) {
						return;
					}
					if (i != headerTab.getCustomDetailPane().getSelectedIndex()) {
						headerTab.getCustomDetailPane().setSelectedIndex(i);
						headerTab.getCustomDetailPane().fireActivateDetailEvent();
					}
					if (adtab.getGridTab().getCurrentRow() != currentRow)
						adtab.getGridTab().setCurrentRow(currentRow, true);
					Executions.schedule(getComponent().getDesktop(), e->((CustomADTabpanel)headerTab).focusToFirstEditor(), new Event("onFocusToHeaderTab"));
					break;
				}
			}
		}
	}

	@Override
	public void addTab(GridTab tab, IADTabpanel tabPanel) {
	}

	@Override
	public void setADWindowPanel(AbstractADWindowContent abstractADWindowPanel) {
	}

}
