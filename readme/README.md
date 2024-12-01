NotePad 应用功能文档

简介

NotePad 是一款简单的 Android 便签应用，提供了笔记的增、删、改、查等基本功能。以下是我为应用添加的四个功能

 

为每条笔记添加时间戳

笔记搜索功能

昼夜模式切换功能

笔记反向排序功能

\1. 为每条笔记添加时间戳

功能描述：

每条笔记都将在创建或修改时自动记录时间戳，便于用户查看最后修改时间。

 

实现方法：

在保存笔记时，自动为每个笔记记录当前的时间戳。这个时间戳将会存储在数据库中的 COLUMN_NAME_MODIFICATION_DATE 字段。

 

代码实现：

在创建或更新笔记时，自动插入当前时间戳：

java

复制代码

// 在保存笔记时更新时间戳

ContentValues values = new ContentValues();

values.put(NotePad.Notes.COLUMN_NAME_TITLE, title);

values.put(NotePad.Notes.COLUMN_NAME_CONTENT, content);

values.put(NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE, System.currentTimeMillis()); // 使用当前时间戳

 

// 插入或更新笔记

getContentResolver().insert(NotePad.Notes.CONTENT_URI, values);

在笔记列表中显示时间戳：

java

复制代码

// 定义数据列和视图ID

String[] dataColumns = { 

  NotePad.Notes.COLUMN_NAME_TITLE, 

  NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE 

};

int[] viewIDs = { 

  android.R.id.text1, 

  R.id.time // 时间戳显示控件

};

 

SimpleCursorAdapter adapter = new SimpleCursorAdapter(

​    this,

​    R.layout.noteslist_item,

​    currentCursor,

​    dataColumns,

​    viewIDs

);

setListAdapter(adapter);

\2. 笔记搜索功能

功能描述：

用户可以根据标题内容对笔记进行搜索，支持模糊匹配查询。

 

实现方法：

提供一个 AlertDialog 弹框，让用户输入搜索关键词，通过查询数据库中的笔记标题进行模糊匹配。

 

代码实现：

启动搜索活动，显示输入框：

java

复制代码

// 启动搜索活动

private void startSearchActivity() {

  AlertDialog.Builder builder = new AlertDialog.Builder(this);

  builder.setTitle("请输入查找内容:");

 

  final EditText input = new EditText(this);

  builder.setView(input);

 

  builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {

​    @Override

​    public void onClick(DialogInterface dialog, int which) {

​      searchQuery = input.getText().toString(); // 获取查询内容

​      performSearch();

​    }

  });

 

  builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {

​    @Override

​    public void onClick(DialogInterface dialog, int which) {

​      dialog.dismiss();

​    }

  });

 

  builder.show();

}

 

// 执行搜索

private void performSearch() {

  if (TextUtils.isEmpty(searchQuery)) {

​    // 如果搜索词为空，显示所有笔记

​    currentCursor = getContentResolver().query(

​        getIntent().getData(),

​        PROJECTION,

​        null, // 不需要过滤条件

​        null,

​        NotePad.Notes.DEFAULT_SORT_ORDER

​    );

  } else {

​    // 执行标题模糊搜索

​    String selection = NotePad.Notes.COLUMN_NAME_TITLE + " LIKE ?";

​    String[] selectionArgs = new String[]{"%" + searchQuery + "%"};

 

​    currentCursor = getContentResolver().query(

​        getIntent().getData(),

​        PROJECTION,

​        selection,

​        selectionArgs,

​        NotePad.Notes.DEFAULT_SORT_ORDER

​    );

  }

 

  // 更新数据源，刷新列表

  if (currentCursor != null && currentCursor.getCount() > 0) {

​    SimpleCursorAdapter adapter = (SimpleCursorAdapter) getListAdapter();

​    adapter.changeCursor(currentCursor);

  } else {

​    currentCursor = null;

​    SimpleCursorAdapter adapter = (SimpleCursorAdapter) getListAdapter();

​    adapter.changeCursor(currentCursor); // 更新为空的数据源

 

​    Toast.makeText(this, "未找到符合条件的笔记", Toast.LENGTH_SHORT).show(); // 提示未找到结果

  }

}

\3. 昼夜模式切换功能

功能描述：

用户可以通过一个按钮切换应用的主题，从白天模式切换到夜间模式（或反之），以适应不同的环境。

 

实现方法：

使用 SharedPreferences 来存储用户的主题偏好，并根据这个偏好切换应用界面的背景色和文字颜色。

 

代码实现：

切换昼夜模式：

java

复制代码

// 切换昼夜模式

private void toggleNightMode() {

  SharedPreferences sharedPreferences = getSharedPreferences("Settings", MODE_PRIVATE);

  SharedPreferences.Editor editor = sharedPreferences.edit();

 

  boolean isNightMode = sharedPreferences.getBoolean("NightMode", false);

 

  if (isNightMode) {

​    editor.putBoolean("NightMode", false);

​    editor.apply();

​    setDayMode(); // 设置为白天模式

  } else {

​    editor.putBoolean("NightMode", true);

​    editor.apply();

​    setNightMode(); // 设置为夜间模式

  }

}

 

// 设置为白天模式

private void setDayMode() {

  // 设置背景为白色

  findViewById(android.R.id.content).setBackgroundColor(Color.WHITE);

 

  // 遍历所有列表项，设置文字颜色为黑色

  ListView listView = getListView();

  for (int i = 0; i < listView.getChildCount(); i++) {

​    View listItem = listView.getChildAt(i);

​    TextView titleTextView = (TextView) listItem.findViewById(android.R.id.text1);

​    TextView timeTextView = (TextView) listItem.findViewById(R.id.time);

 

​    titleTextView.setTextColor(Color.BLACK); // 设置标题文字颜色

​    timeTextView.setTextColor(Color.GRAY);  // 设置时间文字颜色

  }

}

 

// 设置为夜间模式

private void setNightMode() {

  // 设置背景为黑色

  findViewById(android.R.id.content).setBackgroundColor(Color.BLACK);

 

  // 遍历所有列表项，设置文字颜色为白色

  ListView listView = getListView();

  for (int i = 0; i < listView.getChildCount(); i++) {

​    View listItem = listView.getChildAt(i);

​    TextView titleTextView = (TextView) listItem.findViewById(android.R.id.text1);

​    TextView timeTextView = (TextView) listItem.findViewById(R.id.time);

 

​    titleTextView.setTextColor(Color.WHITE); // 设置标题文字颜色

​    timeTextView.setTextColor(Color.LTGRAY); // 设置时间文字颜色

  }

}

\4. 笔记反向排序功能

功能描述：

提供一个功能按钮，让用户可以对笔记进行反向排序，按照修改时间降序或升序排列笔记。

 

实现方法：

使用 ContentResolver 根据修改时间进行排序，并更新 ListView。

 

代码实现：

切换排序顺序：

java

复制代码

private boolean isSortedDescending = false; // 标记当前是否为降序排序

 

// 切换排序顺序

private void toggleSortOrder() {

  String sortOrder = isSortedDescending ? 

​      NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE + " ASC" :

​      NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE + " DESC";

 

  currentCursor = getContentResolver().query(

​      getIntent().getData(),

​      PROJECTION,

​      null, // 不需要过滤条件

​      null,

​      sortOrder // 根据是否反向排序来决定排序顺序

  );

 

  // 更新适配器数据源

  SimpleCursorAdapter adapter = (SimpleCursorAdapter) getListAdapter();

  adapter.changeCursor(currentCursor);

 

  // 切换排序状态

  isSortedDescending = !isSortedDescending;

}



这些功能将使你的便签应用更加实用和便捷。通过为每条笔记添加时间戳，用户可以了解笔记的最后修改时间；通过搜索功能，用户可以快速找到想要的笔记；昼夜模式切换可以让用户在不同的光线条件下舒适使用应用；反向排序功能则提供了一种便捷的笔记查看方式，让用户可以按照修改时间轻松查看最新笔记。