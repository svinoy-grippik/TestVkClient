package ru.msinchevskaya.testvkclient.post;

import java.util.ArrayList;
import java.util.List;

import ru.msinchevskaya.testvkclient.EnterActivity;
import ru.msinchevskaya.testvkclient.R;
import ru.msinchevskaya.testvkclient.auth.Account;
import ru.msinchevskaya.testvkclient.post.FragmentListPost.IListPostListener;
import ru.msinchevskaya.testvkclient.utils.VkItemLoader;
import ru.msinchevskaya.testvkclient.utils.VkItemLoader.IVkItemLoadListener;
import ru.msinchevskaya.testvkclient.vkitems.Post;
import ru.msinchevskaya.testvkclient.vkitems.VkItem;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

public class PostActivity extends ActionBarActivity implements IVkItemLoadListener, IListPostListener{
	
	public static final String INTENT_POST = "post";
	
	private Account mAccount;
	private ArrayList<Post> listPost = new ArrayList<Post>();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_post);
		mAccount = Account.getInstance(this);
		setTitle(Account.getInstance(this).getUser().getFullName());	
		Log.d(getString(R.string.app_tag), getString(R.string.screen_type));
		
	}
	
	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		listPost = savedInstanceState.getParcelableArrayList("ListPost");
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		if (listPost.isEmpty()){
			VkItemLoader itemLoader = VkItemLoader.getInstance(this);
			itemLoader.loadPost(0, PostController.POST_COUNT, VkItemLoader.MODE_LOAD, this);
		}
		else {		
			FragmentListPost listPostFragment = (FragmentListPost) getSupportFragmentManager().findFragmentById(R.id.fragmentList);
			listPostFragment.setRetainInstance(true);
			listPostFragment.update(listPost);
		}
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putParcelableArrayList("ListPost",listPost);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.logout, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()){
		case R.id.action_logout:
			mAccount.removeAccount();
			startEnterActivity();
		}
		return super.onOptionsItemSelected(item);
	}
	
	private void startEnterActivity(){
		Intent intent = new Intent(this, EnterActivity.class);
		startActivity(intent);
		finish();
	}

	@Override
	public void loadingSuccess(List<VkItem> listItem) {
		for (VkItem item : listItem){
			Post post = (Post)item;
			listPost.add(post);
			PostController.addPost(post.getId());
		}
		PostController.setPostVisible(listPost.size());
		FragmentListPost listPostFragment = (FragmentListPost) getSupportFragmentManager().findFragmentById(R.id.fragmentList);
		listPostFragment.update(listPost);
	}

	@Override
	public void loadingError(String message) {
		Log.d(getString(R.string.app_tag), message);
		Toast.makeText(this, getString(R.string.network_error), Toast.LENGTH_LONG).show();
	}
	
	@Override
	public void updateSuccess(List<VkItem> listItem) {
		for (VkItem item : listItem){
			Post post = (Post)item;
			if (PostController.isPostExist(post.getId())){
				FragmentListPost listPostFragment = (FragmentListPost) getSupportFragmentManager().findFragmentById(R.id.fragmentList);
				listPostFragment.update(listPost);
				return;
			}
			listPost.add(0, post); //Добавляем посты в начало списка
			PostController.addPost(post.getId());
		}
		PostController.setPostVisible(listPost.size());
		loadUpdates();
	}

	@Override
	public void loadNext() {
		if (VkItemLoader.getInstance(this).getStatus() == VkItemLoader.STOPPED){
			VkItemLoader itemLoader = VkItemLoader.getInstance(this);
			if (PostController.getPostVisible() != PostController.getTotalPost()){
				if (PostController.getPostVisible() + PostController.POST_COUNT < PostController.getTotalPost()){
					itemLoader.loadPost(PostController.getPostVisible(), PostController.POST_COUNT, VkItemLoader.MODE_LOAD, this);
				}
				else {
					itemLoader.loadPost(PostController.getPostVisible(), PostController.getTotalPost() - PostController.getPostVisible(), VkItemLoader.MODE_LOAD, this);
				}
			}
		}
	}

	@Override
	public void loadUpdates() {
		if (VkItemLoader.getInstance(this).getStatus() == VkItemLoader.STOPPED){
			VkItemLoader itemLoader = VkItemLoader.getInstance(this);
			itemLoader.loadPost(0, PostController.POST_COUNT, VkItemLoader.MODE_UPDATE, this);
		}
	}

	@Override
	public void onPostClick(int position) {
		Log.d(getString(R.string.app_tag), listPost.get(position).getText());
		if ("phone".equals(getString(R.string.screen_type)))
			startFullPostActivity(listPost.get(position));
		else
			addFullPostFragment(listPost.get(position));
	}
	
	private void startFullPostActivity(Post post){
		Intent intent = new Intent(this, FullPostActivity.class);
		intent.putExtra(INTENT_POST, post);
		startActivity(intent);
	}
	
	private void addFullPostFragment(Post post){
		FragmentFullPost fullPost = new FragmentFullPost();		
		Bundle bundle = new Bundle();
		bundle.putParcelable(PostActivity.INTENT_POST, post);
		fullPost.setArguments(bundle);
		getSupportFragmentManager().beginTransaction().replace(R.id.fragmentFull, fullPost).commit();
	}
}
