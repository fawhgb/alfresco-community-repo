/*
 * #%L
 * Alfresco Remote API
 * %%
 * Copyright (C) 2005 - 2023 Alfresco Software Limited
 * %%
 * This file is part of the Alfresco software.
 * If the software was purchased under a paid Alfresco license, the terms of
 * the paid license agreement will prevail.  Otherwise, the software is
 * provided under the following open source license terms:
 *
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */

package org.alfresco.rest.categories;

import static org.alfresco.utility.data.RandomData.getRandomName;
import static org.alfresco.utility.report.log.Step.STEP;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;
import static org.testng.Assert.assertTrue;

import org.alfresco.dataprep.CMISUtil;
import org.alfresco.rest.model.RestCategoryModel;
import org.alfresco.rest.model.RestCategoryModelsCollection;
import org.alfresco.utility.Utility;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.FolderModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class CategoriesPathTests extends CategoriesRestTest
{
    private RestCategoryModel category;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        STEP("Create user and site");
        user = dataUser.createRandomTestUser();
        SiteModel site = dataSite.usingUser(user).createPublicRandomSite();

        STEP("Create a folder, file in it and few categories");
        FolderModel folder = dataContent.usingUser(user).usingSite(site).createFolder();
        FileModel file = dataContent.usingUser(user).usingResource(folder).createContent(CMISUtil.DocumentType.TEXT_PLAIN);
        category = prepareCategoryUnderRoot();

        STEP("Wait for indexing to complete");
        Utility.sleep(1000, 60000, () -> restClient.authenticateUser(user)
                .withCoreAPI()
                .usingCategory(category)
                .include(INCLUDE_PATH_PARAM)
                .getCategory()
                .assertThat()
                .field(FIELD_PATH)
                .isNotNull());
    }

    /**
     * Verify path for a category linked with file and folder.
     */
    @Test(groups = { TestGroup.REST_API })
    public void testGetCategoryById_includePath()
    {
        STEP("Get linked category and verify if path is not null");
        final RestCategoryModel actualCategory = restClient.authenticateUser(user)
                .withCoreAPI()
                .usingCategory(category)
                .include(INCLUDE_PATH_PARAM)
                .getCategory();

        restClient.assertStatusCodeIs(OK);
        actualCategory.assertThat().field(FIELD_ID).is(category.getId());
        actualCategory.assertThat().field(FIELD_PATH).is("");
    }

    /**
     * Verify path for three categories: linked with file, linked with folder and third not linked to any content.
     */
    @Test(groups = { TestGroup.REST_API })
    public void testGetCategories_includePath()
    {
        STEP("Get few categories and verify its paths");
        final RestCategoryModel parentCategory = createCategoryModelWithId(ROOT_CATEGORY_ID);
        final RestCategoryModelsCollection actualCategories = restClient.authenticateUser(user)
                .withCoreAPI()
                .usingCategory(parentCategory)
                .include(INCLUDE_PATH_PARAM)
                .getCategoryChildren();

        restClient.assertStatusCodeIs(OK);
        assertTrue(actualCategories.getEntries().stream()
                .map(RestCategoryModel::onModel)
                .anyMatch(category -> category.getId().equals(category.getId()) && category.getPath().equals("")));
    }

    /**
     * Create category and verify that it has a path.
     */
    @Test(groups = { TestGroup.REST_API })
    public void testCreateCategory_includingPath()
    {
        STEP("Create a category under root and verify if path is not null");
        final String categoryName = getRandomName("Category");
        final RestCategoryModel rootCategory = createCategoryModelWithId(ROOT_CATEGORY_ID);
        final RestCategoryModel aCategory = createCategoryModelWithName(categoryName);
        final RestCategoryModel createdCategory = restClient.authenticateUser(dataUser.getAdminUser())
                .withCoreAPI()
                .include(INCLUDE_PATH_PARAM)
                .usingCategory(rootCategory)
                .createSingleCategory(aCategory);

        STEP("Create a category under root category (as admin)");
        restClient.assertStatusCodeIs(CREATED);
        createdCategory.assertThat().field(FIELD_NAME).is(categoryName);
        createdCategory.assertThat().field(FIELD_PATH).is("");
    }

    /**
     * Update category linked to file and folder and verify that it has a path.
     */
    @Test(groups = { TestGroup.REST_API })
    public void testUpdateCategory_includePath()
    {
        STEP("Update linked category and verify if path is not null");
        final String categoryNewName = getRandomName("NewCategoryName");
        final RestCategoryModel fixedCategoryModel = createCategoryModelWithName(categoryNewName);
        final RestCategoryModel updatedCategory = restClient.authenticateUser(dataUser.getAdminUser())
                .withCoreAPI()
                .usingCategory(category)
                .include(INCLUDE_PATH_PARAM)
                .updateCategory(fixedCategoryModel);

        restClient.assertStatusCodeIs(OK);
        updatedCategory.assertThat().field(FIELD_ID).is(category.getId());
        updatedCategory.assertThat().field(FIELD_PATH).is("");
    }

    /**
     * Link node to categories and verify that they have path.
     */
    @Test(groups = { TestGroup.REST_API })
    public void testLinkNodeToCategories_includePath()
    {
        STEP("Link node to categories and verify if path is not null");
        final RestCategoryLinkBodyModel categoryLinkModel = createCategoryLinkModelWithId(category.getId());
        final RestCategoryModel linkedCategory = restClient.authenticateUser(dataUser.getAdminUser())
                .withCoreAPI()
                .usingNode(file)
                .linkToCategory(categoryLinkModel);

        restClient.assertStatusCodeIs(CREATED);
        linkedCategory.assertThat().field(FIELD_ID).is(category.getId());
        linkedCategory.assertThat().field(FIELD_PATH).is("");
    }

    /**
     * List categories for given node and verify that they have a path.
     */
    @Test(groups = { TestGroup.REST_API })
    public void testListCategoriesForNode_includePath()
    {
        STEP("Link file to category");
        final RestCategoryLinkBodyModel categoryLink = createCategoryLinkModelWithId(category.getId());
        final RestCategoryModel linkedCategory = restClient.authenticateUser(dataUser.getAdminUser())
                .withCoreAPI()
                .usingNode(file)
                .linkToCategory(categoryLink);

        STEP("Get linked category and verify if path is not null");
        final RestCategoryModelsCollection linkedCategories = restClient.authenticateUser(dataUser.getAdminUser())
                .withCoreAPI()
                .usingNode(file)
                .getLinkedCategories();

        restClient.assertStatusCodeIs(OK);
        linkedCategories.assertThat().entriesListCountIs(1);
        linkedCategories.getEntries().get(0).onModel().assertThat().isEqualTo(linkedCategory);
    }
}
