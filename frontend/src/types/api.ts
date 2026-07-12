export type TransactionType = "INCOME" | "EXPENSE";
export type BudgetUsageStatus = "SAFE" | "WARNING" | "OVER_BUDGET";

export interface User {
  id: number;
  email: string;
  fullName: string;
  role?: string;
}

export interface LoginResponse {
  user: User;
  accessToken: string;
  tokenType: string;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

export interface Category {
  id: number;
  name: string;
  type: TransactionType;
}

export interface Transaction {
  id: number;
  type: TransactionType;
  amount: string;
  categoryId: number;
  categoryName: string;
  categoryType: TransactionType;
  description?: string;
  transactionDate: string;
  createdAt: string;
  updatedAt: string;
}

export interface DashboardSummary {
  totalIncome: string;
  totalExpense: string;
  balance: string;
  transactionCount: number;
}

export interface TrendItem {
  period: string;
  incomeAmount: string;
  expenseAmount: string;
  balance: string;
  transactionCount: number;
}

export interface CategoryBreakdownItem {
  categoryId: number;
  categoryName: string;
  type: TransactionType;
  totalAmount: string;
  transactionCount: number;
}

export interface Budget {
  id: number;
  categoryId: number;
  categoryName: string;
  amount: string;
  startDate: string;
  endDate: string;
}

export interface BudgetUsageItem {
  budgetId: number;
  categoryId: number;
  categoryName: string;
  limitAmount: string;
  spentAmount: string;
  remainingAmount: string;
  usagePercentage: string;
  exceeded: boolean;
  status: BudgetUsageStatus;
  startDate: string;
  endDate: string;
}

export type RecurringFrequency = "DAILY" | "WEEKLY" | "MONTHLY" | "YEARLY";

export interface RecurringTransaction {
  id: number;
  type: TransactionType;
  amount: string;
  categoryId: number;
  categoryName: string;
  description?: string;
  frequency: RecurringFrequency;
  startDate: string;
  endDate?: string;
  nextRunDate: string;
  active: boolean;
}

export interface MonthlyReportCategoryItem {
  categoryId: number;
  categoryName: string;
  amount: string;
  transactionCount: number;
}

export interface MonthlyReportDailyTrendItem {
  date: string;
  income: string;
  expense: string;
  balance: string;
  transactionCount: number;
}

export interface MonthlyReport {
  month: string;
  fromDate: string;
  toDate: string;
  totalIncome: string;
  totalExpense: string;
  balance: string;
  transactionCount: number;
  topExpenseCategories: MonthlyReportCategoryItem[];
  topIncomeCategories: MonthlyReportCategoryItem[];
  dailyTrend: MonthlyReportDailyTrendItem[];
}

export interface ImportResult {
  totalRows: number;
  successfulRows: number;
  failedRows: number;
  errors: { rowNumber: number; message: string }[];
}
