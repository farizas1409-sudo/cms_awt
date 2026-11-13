# Screen mapping

| CMS task | Desktop screen | Existing service used |
|---|---|---|
| Sign in | CMS Sign in | `IdentityService` |
| Collections overview | Dashboard | `DashboardService` |
| Find assigned customers | Customer Worklist | `CustomersService` |
| Record a customer contact | Follow-up | `FollowupService` |
| View follow-up activity | Follow-up History | `FollowupService` |
| Record a confirmed payment | Payment Posting | `PaymentService` |
| Review posted payments | Payment History | `HistoryService` |
| Record and review contact attempts | Communications | `CommunicationsService` |
| Manage users and access | User Management, Roles & Permissions | `UsersService`, `RolesService` |
| Maintain lookup data | Application Masters | `MastersService` |
| Configure / execute risk rules | Risk Rules | `RiskService` |
| Assign unallocated customers | Customer Allocation | `AllocationService` |
| View operational reporting | Performance Report, Collector Performance, Collector Reports | `OwnerService`, `CollectorPerformanceService`, `ReportsService` |
| Review security actions | Audit Logs | `AuditService` |
| Change personal settings | Change Password, Appearance Settings | `ProfileService`, `AppearanceService` |

The service and repository layer remains the source of business rules and persistence. AWT screens only collect input, display results, and call those services.
