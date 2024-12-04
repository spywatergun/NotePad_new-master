# NotePad  应用功能文档



NotePad 是一款简单易用的 Android 记事本应用，我为其添加了以下功能：

**主要功能**

1. 添加时间戳

2. 笔记搜索功能

**附加功能**

  1.UI美化
   - 新建笔记按钮改为右下角加号按钮
   - 昼夜模式切换

2. 笔记反向排序功能

**1. 添加时间戳**

**功能**

每条笔记在创建或修改时自动记录时间戳，方便用户查看最后修改时间。

**分析**

1. 在保存笔记时，通过 System.currentTimeMillis() 获取当前时间戳并存储到数据库的 COLUMN_NAME_MODIFICATION_DATE 字段中。
2. 在笔记列表中，利用 SimpleCursorAdapter 将时间戳字段绑定到显示控件上，显示在列表中。

**关键代码**

**记录时间戳**



```
ContentValues values = new ContentValues();

values.put(NotePad.Notes.COLUMN_NAME_TITLE, title);

values.put(NotePad.Notes.COLUMN_NAME_CONTENT, content);

values.put(NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE, System.currentTimeMillis());

getContentResolver().insert(NotePad.Notes.CONTENT_URI, values);


```



**显示时间戳**



```
String[] dataColumns = {

  NotePad.Notes.COLUMN_NAME_TITLE,

  NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE

};

int[] viewIDs = {android.R.id.text1, R.id.time};

SimpleCursorAdapter adapter = new SimpleCursorAdapter(

  this,

  R.layout.noteslist_item,

  currentCursor,

  dataColumns,

  viewIDs

);

setListAdapter(adapter);
```

**将时间戳格式化为日常时间**

```

private String formatTimestamp(long timestamp) {
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    return sdf.format(new Date(timestamp));
}


SimpleCursorAdapter adapter = new SimpleCursorAdapter(
    this,
    R.layout.noteslist_item,
    currentCursor,
    dataColumns,
    viewIDs
);


adapter.setViewBinder((view, cursor, columnIndex) -> {
    if (columnIndex == cursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE)) {
        long timestamp = cursor.getLong(columnIndex);
        String formattedTime = formatTimestamp(timestamp); 
        ((TextView) view).setText(formattedTime);
        return true;
    }
    return false;
});

setListAdapter(adapter);
```

**实现图**

![时间戳](https://github.com/spywatergun/NotePad_new-master/blob/master/readme/%E4%B8%BB%E9%A1%B5.png)




**2. 笔记搜索功能**

**功能**

用户可通过输入关键词，对笔记的标题进行模糊匹配搜索，快速找到所需笔记。

**分析**

1. 使用 AlertDialog 提供搜索输入框，用户输入后提取关键词。
2. 数据库查询中使用 LIKE 关键字进行模糊匹配。
3. 更新查询结果到列表，若无匹配项，则提示用户。

**关键代码**

**搜索对话框**

```
private void startSearchActivity() {

  AlertDialog.Builder builder = new AlertDialog.Builder(this);

  builder.setTitle("请输入查找内容:");

  final EditText input = new EditText(this);

  builder.setView(input);

  builder.setPositiveButton("确定", (dialog, which) -> {

​    searchQuery = input.getText().toString();

​    performSearch();

  });

  builder.setNegativeButton("取消", (dialog, which) -> dialog.dismiss());

  builder.show();

}
```

**执行查询**

```
private void performSearch() {

  String selection = NotePad.Notes.COLUMN_NAME_TITLE + " LIKE ?";

  String[] selectionArgs = new String[]{"%" + searchQuery + "%"};

  currentCursor = getContentResolver().query(

​    getIntent().getData(),

​    PROJECTION,

​    selection,

​    selectionArgs,

​    NotePad.Notes.DEFAULT_SORT_ORDER

  );

  SimpleCursorAdapter adapter = (SimpleCursorAdapter) getListAdapter();

  adapter.changeCursor(currentCursor);

  if (currentCursor == null || currentCursor.getCount() == 0) {

​    Toast.makeText(this, "未找到符合条件的笔记", Toast.LENGTH_SHORT).show();

  }

}
```

**实现图**





搜索成功


![搜索](https://github.com/spywatergun/NotePad_new-master/blob/master/readme/%E6%90%9C%E7%B4%A21.png)
![搜索](https://github.com/spywatergun/NotePad_new-master/blob/master/readme/%E6%90%9C%E7%B4%A22.png)



搜索失败


![搜索](https://github.com/spywatergun/NotePad_new-master/blob/master/readme/%E6%90%9C%E7%B4%A2%E5%A4%B1%E8%B4%A5.png)


**3. UI 美化**

**功能**

1. 将新建笔记按钮更改为右下角悬浮加号按钮，符合 Material Design。
2. 昼夜模式切换，通过按钮切换背景和文字颜色，适应不同光线条件。

**分析**

**(1) 悬浮加号按钮**

- 使用 FloatingActionButton 组件，样式及点击事件跳转至新建笔记界面。

**(2) 昼夜模式切换**

1. 使用 SharedPreferences 存储主题设置。
2. 昼夜模式切换时动态修改界面背景和文字颜色。

**关键代码**

**悬浮加号按钮**

```
<com.google.android.material.floatingactionbutton.FloatingActionButton

  android:id="@+id/fab"

  android:layout_width="wrap_content"

  android:layout_height="wrap_content"

  android:layout_gravity="bottom|end"

  android:layout_margin="16dp"

  app:srcCompat="@drawable/ic_add"

  android:contentDescription="新建笔记" />

FloatingActionButton fab = findViewById(R.id.fab);

fab.setOnClickListener(view -> {

  Intent intent = new Intent(this, NewNoteActivity.class);

  startActivity(intent);

});
```

**昼夜模式切换**

```
private void toggleNightMode() {

  SharedPreferences sharedPreferences = getSharedPreferences("Settings", MODE_PRIVATE);

  SharedPreferences.Editor editor = sharedPreferences.edit();

  boolean isNightMode = sharedPreferences.getBoolean("NightMode", false);

  if (isNightMode) {

​    editor.putBoolean("NightMode", false);

​    setDayMode();

  } else {

​    editor.putBoolean("NightMode", true);

​    setNightMode();

  }

  editor.apply();

}

 

private void setDayMode() {

  findViewById(android.R.id.content).setBackgroundColor(Color.WHITE);

}

 

private void setNightMode() {

  findViewById(android.R.id.content).setBackgroundColor(Color.BLACK);

}
```




**实现图**





悬浮按钮与改进的菜单栏




![悬浮按钮](https://github.com/spywatergun/NotePad_new-master/blob/master/readme/%E4%B8%BB%E9%A1%B5.png)
![菜单栏](https://github.com/spywatergun/NotePad_new-master/blob/master/readme/%E8%8F%9C%E5%8D%95.png)




昼夜模式切换




![昼夜](https://github.com/spywatergun/NotePad_new-master/blob/master/readme/%E5%A4%9C.png)
![昼夜](https://github.com/spywatergun/NotePad_new-master/blob/master/readme/%E6%98%BC.png)





**4. 笔记反向排序功能**

**功能**

用户可以通过按钮切换笔记列表的排序顺序，从修改时间的降序切换到升序（或反之）。

**分析**

1. 使用 ContentResolver 按     COLUMN_NAME_MODIFICATION_DATE 字段进行排序。
2. 每次切换排序后，更新数据源并刷新列表。

**关键代码**

```
private boolean isSortedDescending = false;

 

private void toggleSortOrder() {

  String sortOrder = isSortedDescending ?

​      NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE + " ASC" :

​      NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE + " DESC";

  currentCursor = getContentResolver().query(

​      getIntent().getData(),

​      PROJECTION,

​      null,

​      null,

​      sortOrder

  );

  SimpleCursorAdapter adapter = (SimpleCursorAdapter) getListAdapter();

  adapter.changeCursor(currentCursor);

  isSortedDescending = !isSortedDescending;

}
```





**实现图**




![反向排序](https://github.com/spywatergun/NotePad_new-master/blob/master/readme/%E4%B8%BB%E9%A1%B5.png)
![反向排序](https://github.com/spywatergun/NotePad_new-master/blob/master/readme/%E6%8E%92%E5%BA%8F.png)


 
