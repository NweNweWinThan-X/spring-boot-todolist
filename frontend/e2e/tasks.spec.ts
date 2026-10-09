import { expect, test } from '@playwright/test';
import { quickAdd, register, uniqueAccount } from './helpers';

test.describe('Task management', () => {
  test.beforeEach(async ({ page }) => {
    await register(page, uniqueAccount());
  });

  test('a new account starts with an empty list', async ({ page }) => {
    await expect(page.getByText('No tasks yet')).toBeVisible();
  });

  test('quick add creates a task', async ({ page }) => {
    await quickAdd(page, 'Write the release notes');
    await expect(page.getByTestId('task-card')).toHaveCount(1);
    await expect(page.getByTestId('task-card')).toContainText('Write the release notes');
  });

  test('the checkbox toggles completion and updates progress', async ({ page }) => {
    await quickAdd(page, 'Toggle me');
    const card = page.getByTestId('task-card').first();

    await card.getByRole('checkbox').click();
    await expect(card.getByRole('checkbox')).toHaveAttribute('aria-checked', 'true');
    await expect(card).toContainText('Completed');

    await card.getByRole('checkbox').click();
    await expect(card.getByRole('checkbox')).toHaveAttribute('aria-checked', 'false');
  });

  test('the modal edits every field', async ({ page }) => {
    await quickAdd(page, 'Draft the proposal');
    await page.getByRole('button', { name: 'Edit Draft the proposal' }).click();

    const modal = page.getByTestId('task-modal');
    await expect(modal).toBeVisible();
    await modal.getByLabel('Title').fill('Final proposal');
    await modal.getByLabel('Description').fill('Second revision');
    await modal.getByLabel('Priority').selectOption('HIGH');
    await modal.getByLabel('Due date').fill('2030-01-15');
    await modal.getByLabel('Start time').fill('09:00');
    await modal.getByLabel('End time').fill('11:00');
    await modal.getByRole('button', { name: 'Update' }).click();

    const card = page.getByTestId('task-card').first();
    await expect(card).toContainText('Final proposal');
    await expect(card).toContainText('Second revision');
    await expect(card).toContainText('High');
    await expect(card).toContainText('09:00–11:00');
  });

  test('end time before start time is rejected by the server', async ({ page }) => {
    await quickAdd(page, 'Bad time box');
    await page.getByRole('button', { name: 'Edit Bad time box' }).click();

    const modal = page.getByTestId('task-modal');
    await expect(modal).toBeVisible();
    await modal.getByLabel('Start time').fill('18:00');
    await modal.getByLabel('End time').fill('09:00');
    await modal.getByRole('button', { name: 'Update' }).click();

    await expect(page.getByText('終了時刻は開始時刻より後にしてください')).toBeVisible();
  });

  test('markup in a title is rejected by the allowlist', async ({ page }) => {
    await page.getByTestId('quick-add-input').fill('<script>alert(1)</script>');
    await page.getByTestId('quick-add-submit').click();
    await expect(page.getByText('使用できない文字が含まれています')).toBeVisible();
    await expect(page.getByTestId('task-card')).toHaveCount(0);
  });

  test('delete asks first, then removes the task', async ({ page }) => {
    await quickAdd(page, 'Temporary task');
    await page.getByRole('button', { name: 'Delete Temporary task' }).click();

    await expect(page.getByTestId('confirm-dialog')).toBeVisible();
    await page.getByRole('button', { name: 'Delete', exact: true }).click();

    await expect(page.getByTestId('task-card')).toHaveCount(0);
    await expect(page.getByText('No tasks yet')).toBeVisible();
  });

  test('search narrows the list', async ({ page }) => {
    await quickAdd(page, 'Buy milk');
    await quickAdd(page, 'Call the dentist');
    await expect(page.getByTestId('task-card')).toHaveCount(2);

    await page.getByTestId('search').fill('milk');
    await expect(page.getByTestId('task-card')).toHaveCount(1);
    await expect(page.getByTestId('task-card')).toContainText('Buy milk');
  });

  test('status tabs filter the list', async ({ page }) => {
    await quickAdd(page, 'Still pending');
    await quickAdd(page, 'Already done');
    await page
      .getByTestId('task-card')
      .filter({ hasText: 'Already done' })
      .getByRole('checkbox')
      .click();

    await page.getByRole('tab', { name: 'Completed' }).click();
    await expect(page.getByTestId('task-card')).toHaveCount(1);
    await expect(page.getByTestId('task-card')).toContainText('Already done');

    await page.getByRole('tab', { name: 'Pending' }).click();
    await expect(page.getByTestId('task-card')).toContainText('Still pending');
  });
});
