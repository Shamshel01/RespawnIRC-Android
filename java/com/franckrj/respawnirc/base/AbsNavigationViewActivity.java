package com.franckrj.respawnirc.base;

import android.content.Intent;
import android.content.pm.ShortcutManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.franckrj.respawnirc.ConnectActivity;
import com.franckrj.respawnirc.ConnectAsModoActivity;
import com.franckrj.respawnirc.NavigationMenuAdapter;
import com.franckrj.respawnirc.NavigationMenuListView;
import com.franckrj.respawnirc.R;
import com.franckrj.respawnirc.SettingsActivity;
import com.franckrj.respawnirc.jvcforumlist.SelectForumInListActivity;
import com.franckrj.respawnirc.dialogs.RefreshFavDialogFragment;
import com.franckrj.respawnirc.utils.ThemeManager;
import com.franckrj.respawnirc.utils.JVCParser;
import com.franckrj.respawnirc.utils.PrefsManager;
import com.franckrj.respawnirc.utils.Undeprecator;
import com.franckrj.respawnirc.utils.Utils;

import java.util.ArrayList;

public abstract class AbsNavigationViewActivity extends AbsToolbarActivity implements RefreshFavDialogFragment.NewFavsAvailable {
    protected static final int GROUP_ID_BASIC = 0;
    protected static final int GROUP_ID_FORUM_FAV = 1;
    protected static final int GROUP_ID_TOPIC_FAV = 2;
    protected static final int ITEM_ID_HOME = 0;
    protected static final int ITEM_ID_FORUM = 1;
    protected static final int ITEM_ID_SHOWMP = 2;
    protected static final int ITEM_ID_SHOWGTA = 3;
    protected static final int ITEM_ID_PREF = 4;
    protected static final int ITEM_ID_FORUM_FAV_SELECTED = 5;
    protected static final int ITEM_ID_REFRESH_FORUM_FAV = 6;
    protected static final int ITEM_ID_TOPIC_FAV_SELECTED = 7;
    protected static final int ITEM_ID_REFRESH_TOPIC_FAV = 8;
    protected static final int ITEM_ID_CONNECT = 9;
    protected static final int ITEM_ID_CONNECT_AS_MODO = 10;
    protected static final int MODE_HOME = 0;
    protected static final int MODE_FORUM = 1;
    protected static final int MODE_CONNECT = 2;

    protected static ArrayList<NavigationMenuAdapter.MenuItemInfo> listOfMenuItemInfoForHome = null;
    protected static ArrayList<NavigationMenuAdapter.MenuItemInfo> listOfMenuItemInfoForForum = null;
    protected static ArrayList<NavigationMenuAdapter.MenuItemInfo> listOfMenuItemInfoForConnect = null;
    protected static NavigationMenuAdapter.MenuItemInfo showGTAMenuItem = null;
    protected DrawerLayout layoutForDrawer = null;
    protected NavigationMenuListView navigationMenuList = null;
    protected NavigationMenuAdapter adapterForNavigationMenu = null;
    protected TextView pseudoTextNavigation = null;
    protected ImageView contextConnectImageNavigation = null;
    protected ActionBarDrawerToggle toggleForDrawer = null;
    protected int lastItemSelected = -1;
    protected String pseudoOfUser = "";
    protected boolean isInNavigationConnectMode = false;
    protected String newFavSelected = "";
    protected boolean newFavIsSelectedByLongClick = false;
    protected int idOfBaseActivity = -1;
    protected int currentNavigationMenuMode = -1;
    protected ArrayList<NavigationMenuAdapter.MenuItemInfo> currentListOfMenuItem = null;
    protected String showMpStringContent = "";
    protected boolean backIsOpenDrawer = false;
    protected boolean userIsModo = false;

    protected final AdapterView.OnItemClickListener itemInNavigationClickedListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            int currentItemId = adapterForNavigationMenu.getItemIdOfRow((int) id);
            int currentGroupId = adapterForNavigationMenu.getGroupIdOfRow((int) id);

            if ((currentItemId == ITEM_ID_REFRESH_FORUM_FAV || currentItemId == ITEM_ID_REFRESH_TOPIC_FAV) && currentGroupId == GROUP_ID_BASIC) {
                if (!pseudoOfUser.isEmpty()) {
                    Bundle argForFrag = new Bundle();
                    RefreshFavDialogFragment refreshFavsDialogFragment = new RefreshFavDialogFragment();

                    argForFrag.putString(RefreshFavDialogFragment.ARG_PSEUDO, pseudoOfUser);
                    argForFrag.putString(RefreshFavDialogFragment.ARG_COOKIE_LIST, PrefsManager.getString(PrefsManager.StringPref.Names.COOKIES_LIST));
                    if (currentItemId == ITEM_ID_REFRESH_FORUM_FAV) {
                        argForFrag.putInt(RefreshFavDialogFragment.ARG_FAV_TYPE, RefreshFavDialogFragment.FAV_FORUM);
                    } else {
                        argForFrag.putInt(RefreshFavDialogFragment.ARG_FAV_TYPE, RefreshFavDialogFragment.FAV_TOPIC);
                    }

                    refreshFavsDialogFragment.setArguments(argForFrag);
                    refreshFavsDialogFragment.show(getSupportFragmentManager(), "RefreshFavDialogFragment");
                } else {
                    Toast.makeText(AbsNavigationViewActivity.this, R.string.errorConnectNeeded, Toast.LENGTH_SHORT).show();
                }
            } else if (currentGroupId == GROUP_ID_FORUM_FAV) {
                lastItemSelected = ITEM_ID_FORUM_FAV_SELECTED;
                newFavIsSelectedByLongClick = false;
                newFavSelected = PrefsManager.getStringWithSufix(PrefsManager.StringPref.Names.FORUM_FAV_LINK, String.valueOf(currentItemId));
                newForumOrTopicToRead(newFavSelected, true, false, false);
                layoutForDrawer.closeDrawer(GravityCompat.START);
                adapterForNavigationMenu.setRowSelected((int) id);
            } else if (currentGroupId == GROUP_ID_TOPIC_FAV) {
                lastItemSelected = ITEM_ID_TOPIC_FAV_SELECTED;
                newFavIsSelectedByLongClick = false;
                newFavSelected = PrefsManager.getStringWithSufix(PrefsManager.StringPref.Names.TOPIC_FAV_LINK, String.valueOf(currentItemId));
                newForumOrTopicToRead(newFavSelected, false, false, false);
                layoutForDrawer.closeDrawer(GravityCompat.START);
                adapterForNavigationMenu.setRowSelected((int) id);
            } else {
                lastItemSelected = currentItemId;
                layoutForDrawer.closeDrawer(GravityCompat.START);
                adapterForNavigationMenu.setRowSelected((int) id);
            }
            adapterForNavigationMenu.updateList();
        }
    };

    protected final AdapterView.OnItemLongClickListener itemInNavigationLongClickedListener = new AdapterView.OnItemLongClickListener() {
        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
            int currentItemId = adapterForNavigationMenu.getItemIdOfRow((int) id);
            int currentGroupId = adapterForNavigationMenu.getGroupIdOfRow((int) id);

            if (currentGroupId == GROUP_ID_TOPIC_FAV) {
                lastItemSelected = ITEM_ID_TOPIC_FAV_SELECTED;
                newFavIsSelectedByLongClick = true;
                newFavSelected = PrefsManager.getStringWithSufix(PrefsManager.StringPref.Names.TOPIC_FAV_LINK, String.valueOf(currentItemId));
                newForumOrTopicToRead(newFavSelected, false, false, true);
                layoutForDrawer.closeDrawer(GravityCompat.START);
                adapterForNavigationMenu.setRowSelected((int) id);
                adapterForNavigationMenu.updateList();
                return true;
            }

            return false;
        }
    };

    private final View.OnClickListener headerClickedListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (!pseudoOfUser.isEmpty()) {
                isInNavigationConnectMode = !isInNavigationConnectMode;
                updateNavigationMenu();
            } else {
                lastItemSelected = ITEM_ID_CONNECT;
                layoutForDrawer.closeDrawer(GravityCompat.START);
            }
        }
    };

    private void initializeListsOfMenuItem() {
        if (listOfMenuItemInfoForHome == null || listOfMenuItemInfoForForum == null || listOfMenuItemInfoForConnect == null) {
            listOfMenuItemInfoForHome = new ArrayList<>();
            listOfMenuItemInfoForForum = new ArrayList<>();
            listOfMenuItemInfoForConnect = new ArrayList<>();
            {
                NavigationMenuAdapter.MenuItemInfo tmpItemInfo = new NavigationMenuAdapter.MenuItemInfo();
                tmpItemInfo.textContent = getString(R.string.home);
                tmpItemInfo.drawableResId = R.drawable.ic_action_action_home_dark_zoom;
                tmpItemInfo.isHeader = false;
                tmpItemInfo.isEnabled = true;
                tmpItemInfo.itemId = ITEM_ID_HOME;
                tmpItemInfo.groupId = GROUP_ID_BASIC;
                listOfMenuItemInfoForHome.add(tmpItemInfo);
                listOfMenuItemInfoForForum.add(tmpItemInfo);
            }
            {
                NavigationMenuAdapter.MenuItemInfo tmpItemInfo = new NavigationMenuAdapter.MenuItemInfo();
                tmpItemInfo.textContent = getString(R.string.forum);
                tmpItemInfo.drawableResId = R.drawable.ic_action_communication_forum_dark_zoom;
                tmpItemInfo.isHeader = false;
                tmpItemInfo.isEnabled = true;
                tmpItemInfo.itemId = ITEM_ID_FORUM;
                tmpItemInfo.groupId = GROUP_ID_BASIC;
                listOfMenuItemInfoForForum.add(tmpItemInfo);
            }
            {
                NavigationMenuAdapter.MenuItemInfo tmpItemInfo = new NavigationMenuAdapter.MenuItemInfo();
                tmpItemInfo.textContent = getString(R.string.showMP);
                tmpItemInfo.drawableResId = R.drawable.ic_action_content_mail_dark_zoom;
                tmpItemInfo.isHeader = false;
                tmpItemInfo.isEnabled = false;
                tmpItemInfo.itemId = ITEM_ID_SHOWMP;
                tmpItemInfo.groupId = GROUP_ID_BASIC;
                listOfMenuItemInfoForHome.add(tmpItemInfo);
                listOfMenuItemInfoForForum.add(tmpItemInfo);
            }
            {
                NavigationMenuAdapter.MenuItemInfo tmpItemInfo = new NavigationMenuAdapter.MenuItemInfo();
                tmpItemInfo.textContent = getString(R.string.preference);
                tmpItemInfo.drawableResId = R.drawable.ic_action_action_settings_dark_zoom;
                tmpItemInfo.isHeader = false;
                tmpItemInfo.isEnabled = true;
                tmpItemInfo.itemId = ITEM_ID_PREF;
                tmpItemInfo.groupId = GROUP_ID_BASIC;
                listOfMenuItemInfoForHome.add(tmpItemInfo);
                listOfMenuItemInfoForForum.add(tmpItemInfo);
            }
            {
                NavigationMenuAdapter.MenuItemInfo tmpItemInfo = new NavigationMenuAdapter.MenuItemInfo();
                tmpItemInfo.textContent = getString(R.string.forumFav);
                tmpItemInfo.drawableResId = 0;
                tmpItemInfo.isHeader = true;
                tmpItemInfo.isEnabled = true;
                tmpItemInfo.itemId = -1;
                tmpItemInfo.groupId = -1;
                listOfMenuItemInfoForHome.add(tmpItemInfo);
                listOfMenuItemInfoForForum.add(tmpItemInfo);
            }
            {
                NavigationMenuAdapter.MenuItemInfo tmpItemInfo = new NavigationMenuAdapter.MenuItemInfo();
                tmpItemInfo.textContent = getString(R.string.refresh);
                tmpItemInfo.drawableResId = R.drawable.ic_action_navigation_refresh;
                tmpItemInfo.isHeader = false;
                tmpItemInfo.isEnabled = true;
                tmpItemInfo.itemId = ITEM_ID_REFRESH_FORUM_FAV;
                tmpItemInfo.groupId = GROUP_ID_BASIC;
                listOfMenuItemInfoForHome.add(tmpItemInfo);
                listOfMenuItemInfoForForum.add(tmpItemInfo);
            }
            {
                NavigationMenuAdapter.MenuItemInfo tmpItemInfo = new NavigationMenuAdapter.MenuItemInfo();
                tmpItemInfo.textContent = getString(R.string.topicFav);
                tmpItemInfo.drawableResId = 0;
                tmpItemInfo.isHeader = true;
                tmpItemInfo.isEnabled = true;
                tmpItemInfo.itemId = -1;
                tmpItemInfo.groupId = -1;
                listOfMenuItemInfoForHome.add(tmpItemInfo);
                listOfMenuItemInfoForForum.add(tmpItemInfo);
            }
            {
                NavigationMenuAdapter.MenuItemInfo tmpItemInfo = new NavigationMenuAdapter.MenuItemInfo();
                tmpItemInfo.textContent = getString(R.string.refresh);
                tmpItemInfo.drawableResId = R.drawable.ic_action_navigation_refresh;
                tmpItemInfo.isHeader = false;
                tmpItemInfo.isEnabled = true;
                tmpItemInfo.itemId = ITEM_ID_REFRESH_TOPIC_FAV;
                tmpItemInfo.groupId = GROUP_ID_BASIC;
                listOfMenuItemInfoForHome.add(tmpItemInfo);
                listOfMenuItemInfoForForum.add(tmpItemInfo);
            }
            {
                NavigationMenuAdapter.MenuItemInfo tmpItemInfo = new NavigationMenuAdapter.MenuItemInfo();
                tmpItemInfo.textContent = getString(R.string.connectWithAnotherAccount);
                tmpItemInfo.drawableResId = R.drawable.ic_action_content_add_dark_zoom;
                tmpItemInfo.isHeader = false;
                tmpItemInfo.isEnabled = true;
                tmpItemInfo.itemId = ITEM_ID_CONNECT;
                tmpItemInfo.groupId = GROUP_ID_BASIC;
                listOfMenuItemInfoForConnect.add(tmpItemInfo);
            }
            {
                NavigationMenuAdapter.MenuItemInfo tmpItemInfo = new NavigationMenuAdapter.MenuItemInfo();
                tmpItemInfo.textContent = getString(R.string.connnectAsModoText);
                tmpItemInfo.drawableResId = R.drawable.ic_action_action_empty;
                tmpItemInfo.isHeader = false;
                tmpItemInfo.isEnabled = true;
                tmpItemInfo.itemId = ITEM_ID_CONNECT_AS_MODO;
                tmpItemInfo.groupId = GROUP_ID_BASIC;
                listOfMenuItemInfoForConnect.add(tmpItemInfo);
            }
        }

        if (showGTAMenuItem == null) {
            showGTAMenuItem = new NavigationMenuAdapter.MenuItemInfo();
            showGTAMenuItem.textContent = getString(R.string.gta);
            showGTAMenuItem.drawableResId = R.drawable.ic_action_report_dark_zoom;
            showGTAMenuItem.isHeader = false;
            showGTAMenuItem.isEnabled = true;
            showGTAMenuItem.itemId = ITEM_ID_SHOWGTA;
            showGTAMenuItem.groupId = GROUP_ID_BASIC;
        }
    }

    private void updateNavigationMenu() {
        int newNavigationMenuMode;

        if (!pseudoOfUser.isEmpty()) {
            pseudoTextNavigation.setText(pseudoOfUser);
        } else {
            pseudoTextNavigation.setText(R.string.connectToJVC);
            isInNavigationConnectMode = false;
        }

        if (isInNavigationConnectMode) {
            newNavigationMenuMode = MODE_CONNECT;
        } else {
            newNavigationMenuMode = (idOfBaseActivity == ITEM_ID_FORUM ? MODE_FORUM : MODE_HOME);
        }

        if (newNavigationMenuMode != currentNavigationMenuMode) {
            currentNavigationMenuMode = newNavigationMenuMode;
            switch (currentNavigationMenuMode) {
                case MODE_HOME:
                    currentListOfMenuItem = listOfMenuItemInfoForHome;
                    break;
                case MODE_FORUM:
                    currentListOfMenuItem = listOfMenuItemInfoForForum;
                    break;
                case MODE_CONNECT:
                    currentListOfMenuItem = listOfMenuItemInfoForConnect;
                    break;
                default:
                    currentListOfMenuItem = listOfMenuItemInfoForHome;
                    break;
            }
            adapterForNavigationMenu.setListOfMenuItem(currentListOfMenuItem);
        }

        if (!isInNavigationConnectMode) {
            int positionOfShowMpItem = adapterForNavigationMenu.getPositionDependingOnId(ITEM_ID_SHOWMP, GROUP_ID_BASIC);
            int positionOfShowGTAItem = adapterForNavigationMenu.getPositionDependingOnId(ITEM_ID_SHOWGTA, GROUP_ID_BASIC);

            updateFavsInNavigationMenu(false);
            adapterForNavigationMenu.setRowEnabled(positionOfShowMpItem, !pseudoOfUser.isEmpty());
            adapterForNavigationMenu.setRowSelected(adapterForNavigationMenu.getPositionDependingOnId(idOfBaseActivity, GROUP_ID_BASIC));

            if (!showMpStringContent.isEmpty()) {
                adapterForNavigationMenu.setRowText(positionOfShowMpItem, showMpStringContent);
            }

            if (pseudoOfUser.isEmpty()) {
                contextConnectImageNavigation.setImageDrawable(Undeprecator.resourcesGetDrawable(getResources(), R.drawable.ic_action_content_add_circle_outline));
            } else {
                contextConnectImageNavigation.setImageDrawable(Undeprecator.resourcesGetDrawable(getResources(), R.drawable.ic_action_navigation_expand_more_dark));
            }

            if (userIsModo && !pseudoOfUser.isEmpty()) {
                pseudoTextNavigation.setTextColor(Undeprecator.resourcesGetColor(getResources(), R.color.colorPseudoModoThemeDark));

                if (positionOfShowGTAItem == -1) {
                    int positionOfPrefItem = adapterForNavigationMenu.getPositionDependingOnId(ITEM_ID_PREF, GROUP_ID_BASIC);
                    currentListOfMenuItem.add(positionOfPrefItem, showGTAMenuItem);
                }
            } else {
                pseudoTextNavigation.setTextColor(Color.WHITE);

                if (positionOfShowGTAItem != -1) {
                    currentListOfMenuItem.remove(positionOfShowGTAItem);
                }
            }
        } else {
            adapterForNavigationMenu.setRowSelected(-1);
            contextConnectImageNavigation.setImageDrawable(Undeprecator.resourcesGetDrawable(getResources(), R.drawable.ic_action_navigation_expand_less_dark));
        }

        adapterForNavigationMenu.updateList();
    }

    private void updateFavsInNavigationMenu(boolean needToUpdateAdapter) {
        int currentForumFavArraySize = PrefsManager.getInt(PrefsManager.IntPref.Names.FORUM_FAV_ARRAY_SIZE);
        int currentTopicFavArraySize = PrefsManager.getInt(PrefsManager.IntPref.Names.TOPIC_FAV_ARRAY_SIZE);
        int positionOfRefreshFavItem;

        adapterForNavigationMenu.removeAllItemsFromGroup(GROUP_ID_FORUM_FAV);
        positionOfRefreshFavItem = adapterForNavigationMenu.getPositionDependingOnId(ITEM_ID_REFRESH_FORUM_FAV, GROUP_ID_BASIC);
        for (int i = 0; i < currentForumFavArraySize; ++i) {
            NavigationMenuAdapter.MenuItemInfo tmpItemInfo = new NavigationMenuAdapter.MenuItemInfo();
            tmpItemInfo.textContent = PrefsManager.getStringWithSufix(PrefsManager.StringPref.Names.FORUM_FAV_NAME, String.valueOf(i));
            tmpItemInfo.drawableResId = 0;
            tmpItemInfo.isHeader = false;
            tmpItemInfo.isEnabled = true;
            tmpItemInfo.itemId = i;
            tmpItemInfo.groupId = GROUP_ID_FORUM_FAV;
            currentListOfMenuItem.add(positionOfRefreshFavItem, tmpItemInfo);
            ++positionOfRefreshFavItem;
        }

        adapterForNavigationMenu.removeAllItemsFromGroup(GROUP_ID_TOPIC_FAV);
        positionOfRefreshFavItem = adapterForNavigationMenu.getPositionDependingOnId(ITEM_ID_REFRESH_TOPIC_FAV, GROUP_ID_BASIC);
        for (int i = 0; i < currentTopicFavArraySize; ++i) {
            NavigationMenuAdapter.MenuItemInfo tmpItemInfo = new NavigationMenuAdapter.MenuItemInfo();
            tmpItemInfo.textContent = PrefsManager.getStringWithSufix(PrefsManager.StringPref.Names.TOPIC_FAV_NAME, String.valueOf(i));
            tmpItemInfo.drawableResId = 0;
            tmpItemInfo.isHeader = false;
            tmpItemInfo.isEnabled = true;
            tmpItemInfo.itemId = i;
            tmpItemInfo.groupId = GROUP_ID_TOPIC_FAV;
            currentListOfMenuItem.add(positionOfRefreshFavItem, tmpItemInfo);
            ++positionOfRefreshFavItem;
        }

        if (needToUpdateAdapter) {
            adapterForNavigationMenu.updateList();
        }
    }

    protected void updateMpNumberShowed(String newNumber) {
        int positionOfShowMpItem = adapterForNavigationMenu.getPositionDependingOnId(ITEM_ID_SHOWMP, GROUP_ID_BASIC);

        if (newNumber == null) {
            showMpStringContent = getString(R.string.showMP);
        } else {
            showMpStringContent = getString(R.string.showMPWithNumber, newNumber);
        }

        if (positionOfShowMpItem != -1) {
            adapterForNavigationMenu.setRowText(positionOfShowMpItem, showMpStringContent);
            adapterForNavigationMenu.updateList();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initializeListsOfMenuItem();
        initializeViewAndToolbar();

        pseudoOfUser = PrefsManager.getString(PrefsManager.StringPref.Names.PSEUDO_OF_USER);
        userIsModo = PrefsManager.getBool(PrefsManager.BoolPref.Names.USER_IS_MODO);

        toggleForDrawer = new ActionBarDrawerToggle(this, layoutForDrawer, R.string.openDrawerContentDescRes, R.string.closeDrawerContentDescRes) {
            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                lastItemSelected = -1;
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                if (lastItemSelected != -1) {
                    switch (lastItemSelected) {
                        case ITEM_ID_HOME:
                            if (idOfBaseActivity != ITEM_ID_HOME) {
                                Intent newShowForumIntent = new Intent(AbsNavigationViewActivity.this, SelectForumInListActivity.class);
                                newShowForumIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                startActivity(newShowForumIntent);
                                finish();
                            }
                            break;
                        case ITEM_ID_CONNECT:
                            startActivity(new Intent(AbsNavigationViewActivity.this, ConnectActivity.class));
                            break;
                        case ITEM_ID_CONNECT_AS_MODO:
                            startActivity(new Intent(AbsNavigationViewActivity.this, ConnectAsModoActivity.class));
                            break;
                        case ITEM_ID_SHOWMP:
                            Utils.openLinkInInternalBrowser("http://www.jeuxvideo.com/messages-prives/boite-reception.php", AbsNavigationViewActivity.this);
                            break;
                        case ITEM_ID_SHOWGTA:
                            Utils.openLinkInInternalBrowser("http://www.jeuxvideo.com/gta/hp_alerte.php", AbsNavigationViewActivity.this);
                            break;
                        case ITEM_ID_PREF:
                            startActivity(new Intent(AbsNavigationViewActivity.this, SettingsActivity.class));
                            break;
                        case ITEM_ID_FORUM_FAV_SELECTED:
                            newForumOrTopicToRead(newFavSelected, true, true, newFavIsSelectedByLongClick);
                            newFavSelected = "";
                            break;
                        case ITEM_ID_TOPIC_FAV_SELECTED:
                            newForumOrTopicToRead(newFavSelected, false, true, newFavIsSelectedByLongClick);
                            newFavSelected = "";
                            break;
                    }
                }
                adapterForNavigationMenu.setRowSelected(adapterForNavigationMenu.getPositionDependingOnId(idOfBaseActivity, GROUP_ID_BASIC));
                adapterForNavigationMenu.updateList();

                if (isInNavigationConnectMode) {
                    isInNavigationConnectMode = false;
                    updateNavigationMenu();
                }
            }
        };
        toggleForDrawer.setDrawerSlideAnimationEnabled(false);

        View navigationHeader = getLayoutInflater().inflate(R.layout.navigation_view_header, navigationMenuList, false);
        pseudoTextNavigation = navigationHeader.findViewById(R.id.pseudo_text_navigation_header);
        contextConnectImageNavigation = navigationHeader.findViewById(R.id.context_connect_image_navigation_header);
        adapterForNavigationMenu = new NavigationMenuAdapter(this);
        adapterForNavigationMenu.setBackgroundColors(Undeprecator.resourcesGetColor(getResources(), (ThemeManager.getThemeUsedIsDark() ? android.R.color.white : android.R.color.black)),
                Undeprecator.resourcesGetColor(getResources(), ThemeManager.getColorRes(ThemeManager.ColorName.NAVIGATION_ICON_COLOR)),
                Undeprecator.resourcesGetColor(getResources(), ThemeManager.getColorRes(ThemeManager.ColorName.COLOR_ACCENT)) & 0x40FFFFFF, Undeprecator.resourcesGetColor(getResources(), android.R.color.transparent));
        adapterForNavigationMenu.setFontColors(Undeprecator.resourcesGetColor(getResources(), (ThemeManager.getThemeUsedIsDark() ? android.R.color.white : android.R.color.black)),
                Undeprecator.resourcesGetColor(getResources(), ThemeManager.getColorRes(ThemeManager.ColorName.HEADER_TEXT_COLOR)));
        navigationMenuList.setHeaderView(navigationHeader);
        navigationMenuList.setAdapter(adapterForNavigationMenu);
        navigationMenuList.setOnItemClickListener(itemInNavigationClickedListener);
        navigationMenuList.setOnItemLongClickListener(itemInNavigationLongClickedListener);
        navigationHeader.setOnClickListener(headerClickedListener);
        layoutForDrawer.addDrawerListener(toggleForDrawer);
        layoutForDrawer.setDrawerShadow(ThemeManager.getDrawableRes(ThemeManager.DrawableName.SHADOW_DRAWER), GravityCompat.START);
        updateNavigationMenu();
    }

    @Override
    public void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        toggleForDrawer.syncState();
    }

    @Override
    public void onResume() {
        super.onResume();
        String tmpPseudoOfUser = PrefsManager.getString(PrefsManager.StringPref.Names.PSEUDO_OF_USER);
        boolean tmpUserIsModo = PrefsManager.getBool(PrefsManager.BoolPref.Names.USER_IS_MODO);
        backIsOpenDrawer = PrefsManager.getBool(PrefsManager.BoolPref.Names.BACK_IS_OPEN_DRAWER);

        if (!tmpPseudoOfUser.equals(pseudoOfUser) || tmpUserIsModo != userIsModo) {
            pseudoOfUser = tmpPseudoOfUser;
            userIsModo = tmpUserIsModo;
            updateNavigationMenu();
        }
    }

    @Override
    public void onBackPressed() {
        if (layoutForDrawer.isDrawerOpen(GravityCompat.START)) {
            layoutForDrawer.closeDrawer(GravityCompat.START);
        } else if (backIsOpenDrawer) {
            layoutForDrawer.openDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        toggleForDrawer.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //noinspection SimplifiableIfStatement
        if (toggleForDrawer.onOptionsItemSelected(item)) {
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void getNewFavs(ArrayList<JVCParser.NameAndLink> listOfFavs, int typeOfFav) {
        if (listOfFavs != null) {
            final int sizeOfListOfFavs = listOfFavs.size();
            PrefsManager.IntPref.Names prefFavArraySize;
            PrefsManager.StringPref.Names prefFavName;
            PrefsManager.StringPref.Names prefFavLink;
            int currentFavArraySize;

            if (typeOfFav == RefreshFavDialogFragment.FAV_FORUM) {
                prefFavArraySize = PrefsManager.IntPref.Names.FORUM_FAV_ARRAY_SIZE;
                prefFavName = PrefsManager.StringPref.Names.FORUM_FAV_NAME;
                prefFavLink = PrefsManager.StringPref.Names.FORUM_FAV_LINK;
            } else {
                prefFavArraySize = PrefsManager.IntPref.Names.TOPIC_FAV_ARRAY_SIZE;
                prefFavName = PrefsManager.StringPref.Names.TOPIC_FAV_NAME;
                prefFavLink = PrefsManager.StringPref.Names.TOPIC_FAV_LINK;
            }

            currentFavArraySize = PrefsManager.getInt(prefFavArraySize);

            for (int i = 0; i < currentFavArraySize; ++i) {
                PrefsManager.removeStringWithSufix(prefFavName, String.valueOf(i));
                PrefsManager.removeStringWithSufix(prefFavLink, String.valueOf(i));
            }

            PrefsManager.putInt(prefFavArraySize, sizeOfListOfFavs);

            for (int i = 0; i < sizeOfListOfFavs; ++i) {
                PrefsManager.putStringWithSufix(prefFavName, String.valueOf(i), listOfFavs.get(i).name);
                PrefsManager.putStringWithSufix(prefFavLink, String.valueOf(i), listOfFavs.get(i).link);
            }

            PrefsManager.applyChanges();
            updateFavsInNavigationMenu(true);

            if (typeOfFav == RefreshFavDialogFragment.FAV_FORUM && Build.VERSION.SDK_INT >= 25) {
                ShortcutManager shortcutManager = getSystemService(ShortcutManager.class);
                Utils.updateShortcuts(this, shortcutManager, listOfFavs.size());
            }
        } else {
            Toast.makeText(this, R.string.errorDuringFetchFavs, Toast.LENGTH_SHORT).show();
        }
    }

    protected abstract void initializeViewAndToolbar();
    protected abstract void newForumOrTopicToRead(String link, boolean itsAForum, boolean isWhenDrawerIsClosed, boolean fromLongClick);
}
