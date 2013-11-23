package android.support.v4.app;

import android.support.v4.app.ListFragment;
import android.view.View;

import com.jobmineplus.mobile.R;

public class ListFragmentLayout
{
    public static void setupIds(View view)
    {
        view.findViewById(R.id.empty_id).setId(ListFragment.INTERNAL_EMPTY_ID);
        view.findViewById(R.id.progress_container_id).setId(ListFragment.INTERNAL_PROGRESS_CONTAINER_ID);
        view.findViewById(R.id.list_container_id).setId(ListFragment.INTERNAL_LIST_CONTAINER_ID);
    }
}