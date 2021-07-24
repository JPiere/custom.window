/******************************************************************************
 * Copyright (C) 2008 Low Heng Sin                                            *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 *****************************************************************************/
package custom.window.webui.adwindow;

import org.adempiere.webui.adwindow.IADTabbox;
import org.adempiere.webui.part.UIPart;
import org.compiere.model.GridTab;

/**
 *
 * @author <a href="mailto:hengsin@gmail.com">Low Heng Sin</a>
 *
 */
public interface CustomIADTabbox extends IADTabbox, UIPart {

	/**
	 * @return selected tab panel reference
	 */
	public CustomIADTabpanel getSelectedTabpanel();

	/**
	 *
	 * @param tab
	 * @param tabPanel
	 */
	public void addTab(GridTab tab, CustomIADTabpanel tabPanel);

	/**
	 * @param index
	 * @return IADTabpanel
	 */
	public CustomIADTabpanel getADTabpanel(int index);

	/**
	 * @param gTab
	 * @return IADTabpanel or null if not found
	 */
	public CustomIADTabpanel findADTabpanel(GridTab gTab);

	/**
	 *
	 * @param abstractADWindowPanel
	 */
	public void setADWindowPanel(CustomAbstractADWindowContent abstractADWindowPanel);

	/**
	 * @return the currently selected detail adtabpanel
	 */
	public CustomIADTabpanel getSelectedDetailADTabpanel();

	/**
	 * @return dirty adtabpanel that need save ( if any )
	 */
	public CustomIADTabpanel getDirtyADTabpanel();

}
