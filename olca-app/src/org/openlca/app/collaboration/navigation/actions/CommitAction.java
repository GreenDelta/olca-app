package org.openlca.app.collaboration.navigation.actions;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.openlca.app.M;
import org.openlca.app.collaboration.dialogs.CommitDialog;
import org.openlca.app.collaboration.navigation.NavCache;
import org.openlca.app.db.Database;
import org.openlca.app.db.Repository;
import org.openlca.app.navigation.actions.INavigationAction;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Labels;
import org.openlca.app.util.Question;
import org.openlca.core.database.CategoryDao;
import org.openlca.git.actions.GitCommit;
import org.openlca.git.util.GitUtil;
import org.openlca.util.Strings;

class CommitAction extends Action implements INavigationAction {

	private List<INavigationElement<?>> selection;

	@Override
	public String getText() {
		return M.CommitDots;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return Icon.COMMIT.descriptor();
	}

	@Override
	public boolean isEnabled() {
		return NavCache.get().hasChanges();
	}

	@Override
	public void run() {
		try {
			if (!checkDatabase())
				return;
			var repo = Repository.CURRENT;
			var diffs = repo.diffs.find().withDatabase();
			var input = Datasets.select(selection, diffs, true, false);
			if (input == null || input.action() == CommitDialog.CANCEL)
				return;
			var credentials = repo.promptCredentials();
			var user = credentials != null ? credentials.ident : repo.promptUser();
			if (credentials == null && user == null)
				return;
			var commitId = Actions.runWithCancel(GitCommit.on(repo)
					.changes(input.datasets())
					.withMessage(input.message())
					.as(user));
			if (Strings.nullOrEmpty(commitId))
				return;
			if (input.action() != CommitDialog.COMMIT_AND_PUSH)
				return;
			new PushAction().run(credentials);
		} catch (IOException | GitAPIException | InvocationTargetException | InterruptedException e) {
			Actions.handleException("Error during commit", e);
		} finally {
			Actions.refresh();
		}
	}

	private boolean checkDatabase() {
		var updatedSlashes = checkSlashes();
		if (updatedSlashes != null && !updatedSlashes)
			return false;
		var updatedOthers = checkOthers();
		if (updatedOthers != null && !updatedOthers) {
			if (updatedSlashes != null) {
				Repository.CURRENT.descriptors.reload();
			}
			return false;
		}
		Repository.CURRENT.descriptors.reload();
		return true;
	}

	private Boolean checkSlashes() {
		var dao = new CategoryDao(Database.get());
		var withSlashes = dao.getDescriptors().stream()
				.filter(c -> c.name.contains("/"))
				.toList();
		if (withSlashes.isEmpty())
			return null;
		var message = M.CategoriesContainASlash + "\r\n";
		for (var i = 0; i < Math.min(5, withSlashes.size()); i++) {
			var category = dao.getForId(withSlashes.get(i).id);
			message += "\r\n* " + category.name + " (" + Labels.plural(category.modelType);
			if (category.category != null) {
				message += "/" + category.category.toPath();
			}
			message += ")";
		}
		if (withSlashes.size() > 5) {
			message += "\r\n* " + M.More + " (" + (withSlashes.size() - 5) + ")";
		}
		if (!Question.ask(M.InvalidCategoryNames, message))
			return false;
		for (var descriptor : withSlashes) {
			var category = dao.getForId(descriptor.id);
			category.name = category.name.replace("/", "\\");
			dao.update(category);
		}
		return true;
	}

	private Boolean checkOthers() {
		var dao = new CategoryDao(Database.get());
		var others = dao.getDescriptors().stream()
				.filter(c -> !GitUtil.isValidCategory(c.name))
				.toList();
		if (others.isEmpty())
			return null;
		var message = M.OtherInvalidCategoryNames + "\r\n";
		for (var i = 0; i < Math.min(5, others.size()); i++) {
			var category = dao.getForId(others.get(i).id);
			message += "\r\n* " + category.name + " (" + Labels.plural(category.modelType);
			if (category.category != null) {
				message += "/" + category.category.toPath();
			}
			message += ")";
		}
		if (others.size() > 5) {
			message += "\r\n* " + M.More + " (" + (others.size() - 5) + ")";
		}
		if (!Question.ask(M.InvalidCategoryNames, message))
			return false;
		for (var descriptor : others) {
			var category = dao.getForId(descriptor.id);
			if (category.name.equals(GitUtil.DATASET_SUFFIX)
					|| category.name.equals(GitUtil.BIN_DIR_SUFFIX)
					|| category.name.equals(GitUtil.EMPTY_CATEGORY_FLAG)) {
				category.name = category.name.substring(1);
			} else {
				category.name = category.name
						.replace(GitUtil.DATASET_SUFFIX, " " + GitUtil.DATASET_SUFFIX.substring(1))
						.replace(GitUtil.BIN_DIR_SUFFIX, " " + GitUtil.BIN_DIR_SUFFIX.substring(1))
						.replace(GitUtil.EMPTY_CATEGORY_FLAG, " " + GitUtil.EMPTY_CATEGORY_FLAG.substring(1));
			}
			dao.update(category);
		}
		return true;
	}

	@Override
	public boolean accept(List<INavigationElement<?>> selection) {
		if (!Repository.isConnected())
			return false;
		this.selection = selection;
		return true;
	}

}
