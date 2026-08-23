/** 本地日期(yyyy-MM-dd,本地时区)。statistics / daily-collection 共用。 */
export function todayDateString(): string {
  const now = new Date();
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`;
}
