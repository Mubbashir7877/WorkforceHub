package com.example.employeemanagement.config.ai;

import com.example.employeemanagement.config.ai.HrPolicySeedRunner.DefaultPolicy;
import com.example.employeemanagement.entity.PolicyCategory;

import java.util.List;

/**
 * Content for the starter policy library seeded by HrPolicySeedRunner. These
 * are deliberately generic, average-workplace policies (not legal advice, not
 * tailored to any specific jurisdiction) meant as a reasonable default an
 * HR_ADMIN can edit, replace, or deactivate once real company policy documents
 * are uploaded. A few sections reference this application's own features
 * (the Time Clock, the four platform roles) so the assistant can answer
 * "how do I..." questions about the system itself, not just abstract policy.
 */
final class DefaultPolicies {

    private DefaultPolicies() {}

    static final List<DefaultPolicy> ALL = List.of(
            new DefaultPolicy(
                    "Employee Handbook Overview",
                    "Purpose of the handbook, who it applies to, and how the platform's user roles work.",
                    PolicyCategory.GENERAL,
                    """
                    # Employee Handbook Overview

                    ## Purpose
                    This handbook describes the general policies, expectations, and benefits that
                    apply to all employees of the company. It is meant to answer common day-to-day
                    questions; it does not replace an employee's individual offer letter or
                    employment contract, and where the two conflict, the individual agreement
                    controls. Policies are reviewed periodically and may be updated; the version
                    and effective date on each document reflect the most recent revision.

                    ## Who this applies to
                    These policies apply to all employees, regardless of department or level,
                    unless a specific policy says otherwise (for example, some benefits have a
                    minimum tenure or hours-worked requirement, noted in that policy).

                    ## Platform roles
                    The employee management system distinguishes four roles:

                    - **Employee** — the default role for every staff member. Can view their own
                      profile, clock in/out, view their own time records, and use the HR Assistant.
                    - **Manager** — has an Employee's own access, plus visibility into their team's
                      information where applicable.
                    - **HR Admin** — manages employee records, uploads and maintains official HR
                      policy documents, and administers the knowledge base the HR Assistant answers
                      from.
                    - **System Admin** — manages user accounts, roles, and platform-level
                      configuration. System Admins do not get special access to read other
                      employees' private conversations with the HR Assistant.

                    ## How to get help
                    For questions this handbook or the HR Assistant cannot answer, or for anything
                    involving your specific personal situation (a leave request, a pay discrepancy,
                    a workplace concern), contact HR directly rather than relying solely on the
                    assistant's general answer.

                    ## Using the HR Assistant responsibly
                    The HR Assistant answers using only the official policy documents that have
                    been uploaded and activated by HR. It will not invent policy, will not make
                    employment decisions (hiring, firing, promotion, discipline, compensation), and
                    will not provide legal, medical, tax, or immigration advice. If it says a
                    policy document doesn't cover your question, that means HR hasn't published
                    guidance on that specific topic yet — ask HR directly.
                    """
            ),
            new DefaultPolicy(
                    "Attendance, Punctuality & Time Tracking",
                    "Expected work hours, punctuality, and how to record time using the Time Clock.",
                    PolicyCategory.ATTENDANCE,
                    """
                    # Attendance, Punctuality & Time Tracking

                    ## General expectation
                    Employees are expected to be at work, ready to begin, at their scheduled start
                    time, and to work their full scheduled shift unless approved leave has been
                    arranged in advance with their manager. Consistent, reliable attendance is a
                    basic expectation of employment and is considered as part of performance
                    reviews.

                    ## Recording time with the Time Clock
                    All non-exempt employees must record their working time using the platform's
                    Time Clock feature:

                    - **Clock in** at the start of your shift, before beginning work.
                    - **Clock out** at the end of your shift, and whenever you take an unpaid meal
                      break, if your location requires meal breaks to be recorded separately.
                    - Each clock-in/clock-out pair creates a time clock session tied to your
                      employee record. You can review your own sessions and their status (open or
                      closed) at any time.
                    - If you forget to clock in or out, notify your manager or HR as soon as
                      possible so your record can be corrected. Do not clock in or out on behalf of
                      another employee under any circumstances — this is treated as a serious
                      integrity violation.

                    ## Lateness and absence
                    If you expect to be late or absent, notify your manager before your scheduled
                    start time whenever possible. Repeated unexplained lateness or absence is
                    addressed through the normal performance management process.

                    ## Overtime
                    Non-exempt employees must receive manager approval before working hours beyond
                    their regularly scheduled shift. Unapproved overtime may not be compensated and
                    may result in a conversation with your manager about expectations going
                    forward.

                    ## Remote or field work
                    Employees working outside the primary office location are still expected to
                    clock in and out accurately for the hours actually worked. See the Remote &
                    Hybrid Work policy for additional expectations while working off-site.
                    """
            ),
            new DefaultPolicy(
                    "Paid Time Off, Sick Leave & Holidays",
                    "General guidance on annual leave, sick leave, and observed holidays.",
                    PolicyCategory.LEAVE,
                    """
                    # Paid Time Off, Sick Leave & Holidays

                    ## Paid time off (PTO)
                    Full-time employees accrue paid time off based on tenure, in line with the
                    accrual schedule communicated at hire and shown on individual pay statements.
                    PTO should be requested from your manager as far in advance as reasonably
                    possible, particularly for extended absences, so coverage can be arranged.
                    Unused PTO carryover, payout on separation, and accrual caps are governed by
                    the specific policy in effect in your work location; ask HR if you need the
                    exact figures for your situation.

                    ## Sick leave
                    Employees who are unable to work due to illness or injury, or who need time to
                    care for an immediate family member, should notify their manager as soon as
                    practical, ideally before their shift is scheduled to start. A note from a
                    healthcare provider may be requested for extended absences. Sick leave should
                    not be used as a substitute for planned PTO.

                    ## Company holidays
                    The company publishes an annual holiday calendar identifying paid holidays
                    observed company-wide. Employees required to work on an observed holiday due to
                    business need should coordinate with their manager regarding compensation or
                    time off in lieu, per local practice.

                    ## Leaves of absence
                    Extended leave (medical, parental, bereavement, jury duty, military, or other
                    legally protected leave) is handled case-by-case with HR and is subject to
                    applicable law in your location. Contact HR directly, well in advance where
                    possible, to begin that conversation — this document provides general awareness
                    only and is not a substitute for that individual conversation.

                    ## Requesting time off
                    Submit leave requests to your manager with as much notice as the circumstances
                    allow. Approval is not guaranteed simply because a request was submitted;
                    business needs and prior approved requests from teammates are taken into
                    account.
                    """
            ),
            new DefaultPolicy(
                    "Employee Benefits Overview",
                    "A general overview of the categories of benefits typically offered.",
                    PolicyCategory.BENEFITS,
                    """
                    # Employee Benefits Overview

                    ## Overview
                    Eligible employees may participate in the company's benefits programs, which
                    typically include health coverage, retirement savings, and other supplemental
                    benefits. Specific plan documents, carriers, contribution amounts, and
                    eligibility waiting periods are provided separately during onboarding and open
                    enrollment, since these details change year to year and by location — this
                    overview describes the general categories only.

                    ## Health and wellness
                    Where offered, health benefits may include medical, dental, and vision
                    coverage. Enrollment generally happens during onboarding and during an annual
                    open enrollment period, with qualifying life events (marriage, birth of a
                    child, loss of other coverage) allowing mid-year changes.

                    ## Retirement savings
                    Where offered, a retirement savings plan allows eligible employees to
                    contribute a portion of pay on a pre-tax or post-tax basis, sometimes with a
                    company match. Enrollment, vesting schedules, and match details are provided in
                    the plan's summary plan description.

                    ## Other benefits
                    Depending on location and role, additional benefits may include life insurance,
                    disability coverage, an employee assistance program, or professional
                    development support. Ask HR which of these apply to your specific role and
                    location.

                    ## Questions about your own coverage
                    Because benefits elections and eligibility are personal and vary by plan year,
                    the HR Assistant can describe these programs in general terms but cannot look
                    up or confirm your individual enrollment status, coverage level, or
                    contribution amount — contact HR directly for anything specific to your own
                    account.
                    """
            ),
            new DefaultPolicy(
                    "Code of Conduct & Workplace Behavior",
                    "Expected standards of professional conduct, and how to report concerns.",
                    PolicyCategory.CONDUCT,
                    """
                    # Code of Conduct & Workplace Behavior

                    ## Standard of conduct
                    Employees are expected to treat colleagues, customers, and partners with
                    respect and professionalism. Discrimination or harassment based on a
                    protected characteristic — including but not limited to race, color, religion,
                    sex, national origin, age, disability, or any other characteristic protected by
                    applicable law — will not be tolerated in any form, including in remote or
                    online interactions related to work.

                    ## Equal opportunity
                    The company is an equal opportunity employer. Employment decisions, including
                    hiring, promotion, discipline, and termination, are made based on legitimate
                    business factors, not on protected characteristics.

                    ## Conflicts of interest
                    Employees should avoid situations where personal interests conflict, or appear
                    to conflict, with the interests of the company, and should disclose any
                    potential conflict to their manager or HR.

                    ## Reporting a concern
                    Any employee who experiences or witnesses conduct that may violate this policy
                    should report it to their manager, HR, or through any other channel the company
                    has designated for this purpose. Reports are taken seriously and investigated
                    promptly. Retaliation against anyone who reports a concern in good faith, or who
                    participates in an investigation, is strictly prohibited and is itself grounds
                    for discipline.

                    ## Workplace safety
                    Employees are expected to follow safety procedures applicable to their role and
                    location and to report unsafe conditions promptly.

                    ## Consequences
                    Violations of this policy may result in disciplinary action up to and including
                    termination of employment. Specific disciplinary decisions are made by HR and
                    management following an appropriate review — the HR Assistant does not make or
                    influence these decisions and cannot advise on the outcome of a specific
                    situation.
                    """
            ),
            new DefaultPolicy(
                    "Data Security & Confidentiality",
                    "Expectations for protecting company, customer, and employee data.",
                    PolicyCategory.SECURITY,
                    """
                    # Data Security & Confidentiality

                    ## Confidential information
                    In the course of employment, employees may have access to confidential
                    information about the company, its customers, or fellow employees — including
                    personal data, financial information, and internal business plans. This
                    information must only be used for legitimate business purposes and must not be
                    shared outside the company, or with coworkers who don't have a business need to
                    know, without authorization.

                    ## Account and credential security
                    Employees must not share their login credentials with anyone else, including
                    coworkers, and must not use another employee's account. Use a unique, strong
                    password for company systems, and enable any additional authentication the
                    company requires. Report a lost device, a suspected compromised account, or any
                    suspicious activity to IT/HR immediately.

                    ## Personal data of employees and customers
                    Personal data collected by the company (such as employee records, time clock
                    data, or customer information) is handled according to the principle of least
                    privilege — access is limited to those who need it for their role. Employees
                    should never export, copy, or share personal data outside of authorized company
                    systems.

                    ## Company equipment and systems
                    Company systems and equipment are provided for business use. Reasonable
                    incidental personal use is generally acceptable, but the company reserves the
                    right to monitor use of its systems in accordance with applicable law, and
                    employees should have no expectation of privacy when using company systems.

                    ## Leaving the company
                    Upon separation from the company, all confidential information and company
                    property (physical and digital) must be returned or securely deleted as
                    instructed, and access to company systems will be revoked.

                    ## Reporting a security concern
                    If you suspect a data breach, a phishing attempt, or any other security issue,
                    report it to IT/HR immediately rather than attempting to resolve it yourself.
                    """
            ),
            new DefaultPolicy(
                    "Remote & Hybrid Work Policy",
                    "Expectations for employees working remotely or in a hybrid arrangement.",
                    PolicyCategory.REMOTE_WORK,
                    """
                    # Remote & Hybrid Work Policy

                    ## Eligibility
                    Remote or hybrid work arrangements are approved based on role requirements and
                    business need, at the discretion of an employee's manager and HR. Not all roles
                    are eligible for remote work.

                    ## Expectations while working remotely
                    Employees working remotely are expected to:

                    - Be reachable and responsive during their normal working hours, using the same
                      communication tools used by the rest of the team.
                    - Accurately clock in and out for the hours actually worked, the same as when
                      working on-site — see the Attendance, Punctuality & Time Tracking policy.
                    - Attend scheduled meetings (in person or virtual) as required by their role.
                    - Maintain a reasonably professional, distraction-free work environment for
                      calls and meetings.
                    - Protect company data and equipment to the same standard as in the office — see
                      the Data Security & Confidentiality policy, which applies equally to remote
                      work.

                    ## Equipment
                    Any company equipment provided for remote work remains company property and
                    must be returned upon request or separation from the company. Employees should
                    report equipment issues to IT promptly.

                    ## Changing or ending a remote arrangement
                    Remote or hybrid arrangements may be adjusted by the company based on changing
                    business needs, with reasonable notice where practical. Employees who want to
                    change their existing arrangement should discuss it with their manager.
                    """
            ),
            new DefaultPolicy(
                    "Compensation & Payroll Basics",
                    "General information about pay schedules, deductions, and how to raise pay questions.",
                    PolicyCategory.COMPENSATION,
                    """
                    # Compensation & Payroll Basics

                    ## Pay schedule
                    Employees are paid on a regular schedule communicated at hire (for example,
                    biweekly or semi-monthly, depending on location). Pay statements detailing
                    gross pay, deductions, and net pay are made available each pay period.

                    ## Exempt vs. non-exempt status
                    Employees are classified as exempt or non-exempt in accordance with applicable
                    wage and hour law. Non-exempt employees are paid for all hours actually worked,
                    including any approved overtime, based on their recorded time clock sessions.
                    Exempt employees receive a fixed salary that is not directly tied to hours
                    worked in a given week.

                    ## Deductions
                    Standard payroll deductions include applicable taxes and any benefits
                    contributions the employee has elected. Any other deduction requires the
                    employee's authorization except where required by law.

                    ## Pay discrepancies
                    If you believe there is an error in your pay, contact HR or payroll directly,
                    as soon as possible, so it can be investigated and corrected. The HR Assistant
                    can explain how pay and deductions generally work, but it cannot look up,
                    confirm, or correct an individual employee's actual pay figures.

                    ## Raises, bonuses, and promotions
                    Decisions about individual compensation changes, bonuses, and promotions are
                    made by management and HR based on performance, role, and business factors, and
                    are communicated directly to the employee. The HR Assistant does not have
                    visibility into, and cannot discuss, any specific employee's individual
                    compensation decisions.
                    """
            )
    );
}
