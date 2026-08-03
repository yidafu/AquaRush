export function formatAdminTableAmount(amount?: number | string | null) {
  if (typeof amount === 'number') {
    return (amount / 100).toFixed(2)
  }
  if (typeof amount === 'string') {
    return (Number(amount) / 100).toFixed(2)
  }
  return '0'
}
