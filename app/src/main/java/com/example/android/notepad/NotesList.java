package com.example.android.notepad;

import android.app.ListActivity;
import android.content.ClipboardManager;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;
import android.widget.EditText;
import android.content.DialogInterface;
import android.app.AlertDialog;
import android.widget.TextView;
import android.graphics.Color;

import android.view.ContextMenu.ContextMenuInfo;

public class NotesList extends ListActivity {

    private static final String TAG = "NotesList";

    // 查询笔记所需的投影列
    private static final String[] PROJECTION = new String[] {
            NotePad.Notes._ID,
            NotePad.Notes.COLUMN_NAME_TITLE,
            NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE,
    };

    private static final int COLUMN_INDEX_TITLE = 1; // 标题列的索引

    private String searchQuery = ""; // 用于保存搜索查询的字符串
    private Cursor currentCursor; // 当前的数据光标

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_note_list);  // 确保这里加载的是正确的布局文件

        setDefaultKeyMode(DEFAULT_KEYS_SHORTCUT); // 设置默认快捷键模式

        Intent intent = getIntent();
        if (intent.getData() == null) {
            intent.setData(NotePad.Notes.CONTENT_URI); // 默认加载所有笔记
        }

        getListView().setOnCreateContextMenuListener(this); // 为列表设置上下文菜单

        // 初始加载所有数据
        currentCursor = managedQuery(
                getIntent().getData(),
                PROJECTION,
                null,
                null,
                NotePad.Notes.DEFAULT_SORT_ORDER
        );

        String[] dataColumns = { NotePad.Notes.COLUMN_NAME_TITLE, NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE };
        int[] viewIDs = { android.R.id.text1, R.id.time };

        // 设置适配器，绑定数据
        SimpleCursorAdapter adapter = new SimpleCursorAdapter(
                this,
                R.layout.noteslist_item,
                currentCursor,
                dataColumns,
                viewIDs
        );

        setListAdapter(adapter);

        // 获取按钮并设置点击事件
        ImageButton fab = (ImageButton) findViewById(R.id.fab_add_note);  // 获取按钮
        if (fab != null) {
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // 点击按钮时启动新建笔记的 Activity
                    Intent createNoteIntent = new Intent(Intent.ACTION_INSERT, getIntent().getData());
                    startActivity(createNoteIntent);
                }
            });
        } else {
            Log.e("NotesList", "ImageButton fab_add_note not found!");  // 如果按钮未找到，输出错误信息
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.list_options_menu, menu); // 加载菜单

        // 为反向排序菜单项添加选项
        // ... 目前暂未实现

        // 为搜索菜单项添加替代菜单项
        Intent intent = new Intent(null, getIntent().getData());
        intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
        menu.addIntentOptions(Menu.CATEGORY_ALTERNATIVE, 0, 0,
                new ComponentName(this, NotesList.class), null, intent, 0, null);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        MenuItem mPasteItem = menu.findItem(R.id.menu_paste);  // 获取粘贴菜单项
        if (clipboard.hasPrimaryClip()) {
            mPasteItem.setEnabled(true);  // 如果剪贴板有内容，启用粘贴项
        } else {
            mPasteItem.setEnabled(false);  // 如果剪贴板为空，禁用粘贴项
        }

        final boolean haveItems = getListAdapter().getCount() > 0;
        if (haveItems) {
            Uri uri = ContentUris.withAppendedId(getIntent().getData(), getSelectedItemId());
            Intent[] specifics = new Intent[1];
            specifics[0] = new Intent(Intent.ACTION_EDIT, uri);

            MenuItem[] items = new MenuItem[1];
            Intent intent = new Intent(null, uri);
            intent.addCategory(Intent.CATEGORY_ALTERNATIVE);

            menu.addIntentOptions(
                    Menu.CATEGORY_ALTERNATIVE,
                    Menu.NONE,
                    Menu.NONE,
                    null,
                    specifics,
                    intent,
                    Menu.NONE,
                    items
            );

            if (items[0] != null) {
                items[0].setShortcut('1', 'e');
            }
        } else {
            menu.removeGroup(Menu.CATEGORY_ALTERNATIVE);  // 如果没有项，移除相关菜单项
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_paste:  // 处理粘贴
                startActivity(new Intent(Intent.ACTION_PASTE, getIntent().getData()));
                return true;
            case R.id.menu_search:  // 处理搜索按钮
                startSearchActivity();
                return true;
            case R.id.menu_toggle_theme:  // 处理切换昼夜模式
                toggleNightMode();
                return true;
            case R.id.menu_reverse_sort:  // 处理反向排序
                toggleSortOrder();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private boolean isSortedDescending = false; // 标记当前是否为降序排序

    // 切换排序顺序
    private void toggleSortOrder() {
        String sortOrder = isSortedDescending ?
                NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE + " ASC" :
                NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE + " DESC";

        currentCursor = getContentResolver().query(
                getIntent().getData(),
                PROJECTION,
                null,  // 不需要过滤条件
                null,
                sortOrder  // 根据是否反向排序来决定排序顺序
        );

        // 更新适配器数据源
        SimpleCursorAdapter adapter = (SimpleCursorAdapter) getListAdapter();
        adapter.changeCursor(currentCursor);

        // 切换排序状态
        isSortedDescending = !isSortedDescending;
    }

    // 切换昼夜模式
    private void toggleNightMode() {
        SharedPreferences sharedPreferences = getSharedPreferences("Settings", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        boolean isNightMode = sharedPreferences.getBoolean("NightMode", false);

        if (isNightMode) {
            editor.putBoolean("NightMode", false);
            editor.apply();
            setDayMode();  // 设置为白天模式
        } else {
            editor.putBoolean("NightMode", true);
            editor.apply();
            setNightMode();  // 设置为夜间模式
        }
    }

    // 设置为白天模式
    private void setDayMode() {
        // 设置背景为白色
        findViewById(android.R.id.content).setBackgroundColor(Color.WHITE);

        // 遍历所有列表项，设置文字颜色为黑色
        ListView listView = getListView();
        for (int i = 0; i < listView.getChildCount(); i++) {
            View listItem = listView.getChildAt(i);
            TextView titleTextView = (TextView) listItem.findViewById(android.R.id.text1);
            TextView timeTextView = (TextView) listItem.findViewById(R.id.time);

            titleTextView.setTextColor(Color.BLACK);  // 设置标题文字颜色
            timeTextView.setTextColor(Color.GRAY);    // 设置时间文字颜色
        }
    }

    // 设置为夜间模式
    private void setNightMode() {
        // 设置背景为黑色
        findViewById(android.R.id.content).setBackgroundColor(Color.BLACK);

        // 遍历所有列表项，设置文字颜色为白色
        ListView listView = getListView();
        for (int i = 0; i < listView.getChildCount(); i++) {
            View listItem = listView.getChildAt(i);
            TextView titleTextView = (TextView) listItem.findViewById(android.R.id.text1);
            TextView timeTextView = (TextView) listItem.findViewById(R.id.time);

            titleTextView.setTextColor(Color.WHITE);  // 设置标题文字颜色
            timeTextView.setTextColor(Color.LTGRAY);  // 设置时间文字颜色
        }
    }

    // 启动搜索活动
    private void startSearchActivity() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("请输入查找内容:");

        final EditText input = new EditText(this);
        builder.setView(input);

        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                searchQuery = input.getText().toString();  // 获取查询内容
                performSearch();
            }
        });

        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        builder.show();
    }

    // 执行搜索
    private void performSearch() {
        if (TextUtils.isEmpty(searchQuery)) {
            // 如果搜索词为空，显示所有笔记
            currentCursor = getContentResolver().query(
                    getIntent().getData(),
                    PROJECTION,
                    null, // 不需要过滤条件
                    null,
                    NotePad.Notes.DEFAULT_SORT_ORDER
            );
        } else {
            // 执行标题模糊搜索
            String selection = NotePad.Notes.COLUMN_NAME_TITLE + " LIKE ?";
            String[] selectionArgs = new String[]{"%" + searchQuery + "%"};

            currentCursor = getContentResolver().query(
                    getIntent().getData(),
                    PROJECTION,
                    selection,
                    selectionArgs,
                    NotePad.Notes.DEFAULT_SORT_ORDER
            );
        }

        // 更新数据源，刷新列表
        if (currentCursor != null && currentCursor.getCount() > 0) {
            SimpleCursorAdapter adapter = (SimpleCursorAdapter) getListAdapter();
            adapter.changeCursor(currentCursor);
        } else {
            currentCursor = null;
            SimpleCursorAdapter adapter = (SimpleCursorAdapter) getListAdapter();
            adapter.changeCursor(currentCursor);  // 更新为空的数据源

            Toast.makeText(this, "未找到符合条件的笔记", Toast.LENGTH_SHORT).show();  // 提示未找到结果
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {
        AdapterView.AdapterContextMenuInfo info;
        try {
            info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        } catch (ClassCastException e) {
            Log.e(TAG, "bad menuInfo", e);
            return;
        }

        Cursor cursor = (Cursor) getListAdapter().getItem(info.position);
        if (cursor == null) {
            return;
        }

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.list_context_menu, menu);

        menu.setHeaderTitle(cursor.getString(COLUMN_INDEX_TITLE));

        Intent intent = new Intent(null, Uri.withAppendedPath(getIntent().getData(),
                Integer.toString((int) info.id)));
        intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        menu.addIntentOptions(Menu.CATEGORY_ALTERNATIVE, 0, 0,
                new ComponentName(this, NotesList.class), null, intent, 0, null);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info;
        try {
            info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        } catch (ClassCastException e) {
            Log.e(TAG, "bad menuInfo", e);
            return false;
        }

        Uri noteUri = ContentUris.withAppendedId(getIntent().getData(), info.id);

        switch (item.getItemId()) {
            case R.id.context_open:
                startActivity(new Intent(Intent.ACTION_EDIT, noteUri));
                return true;
            case R.id.context_copy:
                ClipboardManager clipboard = (ClipboardManager)
                        getSystemService(Context.CLIPBOARD_SERVICE);
                clipboard.setPrimaryClip(ClipData.newUri(
                        getContentResolver(),
                        "Note",
                        noteUri)
                );
                return true;
            case R.id.context_delete:
                getContentResolver().delete(
                        noteUri,
                        null,
                        null
                );
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        Uri uri = ContentUris.withAppendedId(getIntent().getData(), id);
        String action = getIntent().getAction();

        if (Intent.ACTION_PICK.equals(action) || Intent.ACTION_GET_CONTENT.equals(action)) {
            setResult(RESULT_OK, new Intent().setData(uri));  // 如果是选择返回，设置结果
        } else {
            startActivity(new Intent(Intent.ACTION_EDIT, uri));  // 否则，编辑笔记
        }
    }
}
