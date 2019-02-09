package com.franckrj.respawnirc.jvcforum;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.cardview.widget.CardView;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.franckrj.respawnirc.R;
import com.franckrj.respawnirc.base.AbsHomeIsBackActivity;
import com.franckrj.respawnirc.base.AbsWebRequestAsyncTask;
import com.franckrj.respawnirc.utils.JVCParser;
import com.franckrj.respawnirc.utils.PrefsManager;
import com.franckrj.respawnirc.utils.Undeprecator;
import com.franckrj.respawnirc.utils.Utils;
import com.franckrj.respawnirc.utils.WebManager;

import java.util.ArrayList;

public class ShowForumInfosActivity extends AbsHomeIsBackActivity {
    public static final String EXTRA_FORUM_LINK = "com.franckrj.respawnirc.showforuminfos.EXTRA_FORUM_LINK";
    public static final String EXTRA_COOKIES = "com.franckrj.respawnirc.showforuminfos.EXTRA_COOKIES";

    private static final String SAVE_FORUM_INFOS = "saveForumInfos";
    private static final String SAVE_SCROLL_POSITION = "saveScrollPosition";

    private TextView backgroundErrorText = null;
    private SwipeRefreshLayout swipeRefresh = null;
    private ScrollView mainScrollView = null;
    private LinearLayout mainLayout = null;
    private TextView numberOfConnectedView = null;
    private CardView subforumsCardView = null;
    private TextView listOfModeratorsText = null;
    private LinearLayout layoutListOfSubforums = null;
    private DownloadForumInfos currentTaskForDownload = null;
    private ForumInfos infosForForum = null;

    private final View.OnClickListener subforumButtonClickedListener = new View.OnClickListener() {
        @Override
        public void onClick(View button) {
            if (button.getTag() != null && button.getTag() instanceof String) {
                Intent newShowForumIntent = new Intent(ShowForumInfosActivity.this, ShowForumActivity.class);
                newShowForumIntent.putExtra(ShowForumActivity.EXTRA_NEW_LINK, (String) button.getTag());
                newShowForumIntent.putExtra(ShowForumActivity.EXTRA_IS_FIRST_ACTIVITY, false);
                startActivity(newShowForumIntent);
            }
        }
    };

    private final View.OnClickListener contactModeratorsButtonClickedListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (infosForForum != null && !infosForForum.listOfModeratorsString.isEmpty()) {
                Utils.openLinkInInternalBrowser("http://www.jeuxvideo.com/messages-prives/nouveau.php?all_dest=" + infosForForum.listOfModeratorsString.replace(", ", ";"), ShowForumInfosActivity.this);
            } else {
                Toast.makeText(ShowForumInfosActivity.this, R.string.errorDuringContactModerators, Toast.LENGTH_SHORT).show();
            }
        }
    };

    private final View.OnClickListener showForumRulesButtonClickedListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (getIntent() != null && getIntent().getStringExtra(EXTRA_FORUM_LINK) != null) {
                PrefsManager.LinkType linkTypeForInternalBrowser = new PrefsManager.LinkType(PrefsManager.LinkType.NO_LINKS);
                String forumLink = getIntent().getStringExtra(EXTRA_FORUM_LINK);

                linkTypeForInternalBrowser.setTypeFromString(PrefsManager.getString(PrefsManager.StringPref.Names.LINK_TYPE_FOR_INTERNAL_BROWSER));
                Utils.openCorrespondingBrowser(linkTypeForInternalBrowser, "http://www.jeuxvideo.com/forums/" + JVCParser.getForumNameOfThisForum(forumLink) + "/regles-forum/" + JVCParser.getForumIdOfThisForum(forumLink), ShowForumInfosActivity.this);
            } else {
                Toast.makeText(ShowForumInfosActivity.this, R.string.errorDuringShowRules, Toast.LENGTH_SHORT).show();
            }
        }
    };

    private final AbsWebRequestAsyncTask.RequestIsStarted requestIsStartedListener = new AbsWebRequestAsyncTask.RequestIsStarted() {
        @Override
        public void onRequestIsStarted() {
            backgroundErrorText.setVisibility(View.GONE);
            swipeRefresh.setRefreshing(true);
        }
    };

    private final AbsWebRequestAsyncTask.RequestIsFinished<ForumInfos> downloadInfosIsFinishedListener = new AbsWebRequestAsyncTask.RequestIsFinished<ForumInfos>() {
        @Override
        public void onRequestIsFinished(ForumInfos newInfos) {
            swipeRefresh.setRefreshing(false);

            if (newInfos != null) {
                infosForForum = newInfos;
                updateDisplayedInfos();
            } else {
                backgroundErrorText.setVisibility(View.VISIBLE);
            }

            currentTaskForDownload = null;
        }
    };

    private void updateDisplayedInfos() {
        if (infosForForum == null) {
            mainLayout.setVisibility(View.GONE);
        } else {
            mainLayout.setVisibility(View.VISIBLE);
            if (!infosForForum.listOfSubforums.isEmpty()) {
                subforumsCardView.setVisibility(View.VISIBLE);
                for (JVCParser.NameAndLink nameAndLink : infosForForum.listOfSubforums) {
                    Button newSubforumButton = (Button) getLayoutInflater().inflate(R.layout.button_subforum, layoutListOfSubforums, false);

                    newSubforumButton.setText(Undeprecator.htmlFromHtml(nameAndLink.name));
                    newSubforumButton.setTag(nameAndLink.link);
                    newSubforumButton.setOnClickListener(subforumButtonClickedListener);

                    layoutListOfSubforums.addView(newSubforumButton);
                }
            } else {
                subforumsCardView.setVisibility(View.GONE);
            }
            if (!infosForForum.numberOfConnected.isEmpty()) {
                numberOfConnectedView.setText(Undeprecator.htmlFromHtml(infosForForum.numberOfConnected));
            } else {
                numberOfConnectedView.setText(R.string.errorNumberConnected);
            }
            if (!infosForForum.listOfModeratorsString.isEmpty()) {
                listOfModeratorsText.setText(getString(R.string.listOfModeratorsText, infosForForum.listOfModeratorsString));
            } else {
                listOfModeratorsText.setText(R.string.listOfModeratorsTextEmpty);
            }
        }
    }

    private void stopAllCurrentTasks() {
        if (currentTaskForDownload != null) {
            currentTaskForDownload.clearListenersAndCancel();
            currentTaskForDownload = null;
        }
        swipeRefresh.setRefreshing(false);
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_showforuminfos);
        initToolbar(R.id.toolbar_showforuminfos);

        Button contactModeratorsButton = findViewById(R.id.contactmoderators_button_showforuminfos);
        Button showForumRulesButton = findViewById(R.id.showforumrules_button_showforuminfos);

        backgroundErrorText = findViewById(R.id.text_errorbackgroundmessage_showforuminfos);
        swipeRefresh = findViewById(R.id.swiperefresh_showforuminfos);
        mainScrollView = findViewById(R.id.scrollview_showforuminfos);
        mainLayout = findViewById(R.id.main_layout_showforuminfos);
        numberOfConnectedView = findViewById(R.id.text_numberofconnected_showforuminfos);
        subforumsCardView = findViewById(R.id.subforum_card_showforuminfos);
        layoutListOfSubforums = findViewById(R.id.subforum_list_showforuminfos);
        listOfModeratorsText = findViewById(R.id.listofmoderators_text_showforuminfos);

        contactModeratorsButton.setOnClickListener(contactModeratorsButtonClickedListener);
        showForumRulesButton.setOnClickListener(showForumRulesButtonClickedListener);

        backgroundErrorText.setVisibility(View.GONE);
        swipeRefresh.setEnabled(false);
        swipeRefresh.setColorSchemeResources(R.color.colorControlHighlightThemeLight);
        mainLayout.setVisibility(View.GONE);

        if (savedInstanceState != null) {
            infosForForum = savedInstanceState.getParcelable(SAVE_FORUM_INFOS);
            updateDisplayedInfos();

            mainScrollView.post(new Runnable() {
                @Override
                public void run() {
                    mainScrollView.scrollTo(0, savedInstanceState.getInt(SAVE_SCROLL_POSITION, 0));
                }
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (infosForForum == null) {
            if (getIntent() != null) {
                if (getIntent().getStringExtra(EXTRA_FORUM_LINK) != null && getIntent().getStringExtra(EXTRA_COOKIES) != null) {
                    currentTaskForDownload = new DownloadForumInfos();
                    currentTaskForDownload.setRequestIsStartedListener(requestIsStartedListener);
                    currentTaskForDownload.setRequestIsFinishedListener(downloadInfosIsFinishedListener);
                    currentTaskForDownload.execute(getIntent().getStringExtra(EXTRA_FORUM_LINK), getIntent().getStringExtra(EXTRA_COOKIES));
                }
            }

            if (currentTaskForDownload == null) {
                backgroundErrorText.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public void onPause() {
        stopAllCurrentTasks();
        super.onPause();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(SAVE_FORUM_INFOS, infosForForum);
        outState.putInt(SAVE_SCROLL_POSITION, mainScrollView.getScrollY());
    }

    private static class DownloadForumInfos extends AbsWebRequestAsyncTask<String, Void, ForumInfos> {
        @Override
        protected ForumInfos doInBackground(String... params) {
            if (params.length > 1) {
                WebManager.WebInfos currentWebInfos = initWebInfos(params[1], false);
                String source = WebManager.sendRequest(params[0], "GET", "", currentWebInfos);

                if (source != null && !source.isEmpty()) {
                    ForumInfos newForumInfos = new ForumInfos();

                    newForumInfos.listOfSubforums = JVCParser.getListOfSubforumsInForumPage(source);
                    newForumInfos.numberOfConnected = JVCParser.getNumberOfConnectFromPage(source);
                    newForumInfos.listOfModeratorsString = JVCParser.getListOfModeratorsFromPage(source);
                    return newForumInfos;
                }
            }
            return null;
        }
    }

    private static class ForumInfos implements Parcelable {
        public String numberOfConnected = "";
        public ArrayList<JVCParser.NameAndLink> listOfSubforums = new ArrayList<>();
        public String listOfModeratorsString = "";

        public static final Parcelable.Creator<ForumInfos> CREATOR = new Parcelable.Creator<ForumInfos>() {
            @Override
            public ForumInfos createFromParcel(Parcel in) {
                return new ForumInfos(in);
            }

            @Override
            public ForumInfos[] newArray(int size) {
                return new ForumInfos[size];
            }
        };

        public ForumInfos() {
            //rien
        }

        private ForumInfos(Parcel in) {
            numberOfConnected = in.readString();
            in.readTypedList(listOfSubforums, JVCParser.NameAndLink.CREATOR);
            listOfModeratorsString = in.readString();
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            out.writeString(numberOfConnected);
            out.writeTypedList(listOfSubforums);
            out.writeString(listOfModeratorsString);
        }
    }
}
