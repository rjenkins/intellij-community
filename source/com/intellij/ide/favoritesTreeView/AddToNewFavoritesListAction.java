package com.intellij.ide.favoritesTreeView;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.project.Project;

/**
 * User: anna
 * Date: Feb 28, 2005
 */
class AddToNewFavoritesListAction extends AnAction {
 public AddToNewFavoritesListAction() {
   super("Add To New Favorites List");
 }

 public void actionPerformed(AnActionEvent e) {
   final DataContext dataContext = e.getDataContext();
   final AddNewFavoritesListAction action = (AddNewFavoritesListAction)ActionManager.getInstance().getAction(IdeActions.ADD_NEW_FAVORITES_LIST);
   final FavoritesTreeViewPanel favoritesTreeViewPanel = action.doAddNewFavoritesList((Project)dataContext.getData(DataConstants.PROJECT));
   new AddToFavoritesAction(favoritesTreeViewPanel.getName()).actionPerformed(e);
 }
}
