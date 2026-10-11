const calendarElement = document.getElementById("dp");
// shortcut: Singapore date is captured at page load; refresh after midnight.
const singaporeToday = calendarElement.dataset.today;
const calendarMessage = document.getElementById("calendar-message");
const myTrainingButton = document.getElementById("my-training");
const teamTrainingButton = document.getElementById("team-training");
const allStaffButton = document.getElementById("all-staff-training");
const categorySelect = document.getElementById("calendar-category");
const monthSelect = document.getElementById("calendar-month");
const yearSelect = document.getElementById("calendar-year");
const trainingDetails = document.getElementById("training-details");
const courseList = document.getElementById("course-list");
const courseListHeading = document.getElementById("course-list-heading");

const categoryLabels = {
  INTERNAL: "Internal Training",
  EXTERNAL: "External Course",
  PROFESSIONAL: "Professional Certification"
};

const durationLabels = {
  FULLDAY: "Full day",
  HALFDAYAM: "Half day (AM)",
  HALFDAYPM: "Half day (PM)"
};

let allStaff = false;
let teamTraining = false;
let latestRequest = 0;

const categoryColours = {
  INTERNAL: {
    solid: "#166534",
    pale: "#dcfce7"
  },
  EXTERNAL: {
    solid: "#1e40af",
    pale: "#dbeafe"
  },
  PROFESSIONAL: {
    solid: "#6b21a8",
    pale: "#f3e8ff"
  }
};

const calendar = new DayPilot.Month("dp", {
  startDate: calendarElement.dataset.start,
  weekStarts: 0,
  eventHeight: 38,
  xssProtection: "Enabled",

  // Calendar viewing must not change application dates.
  eventMoveHandling: "Disabled",
  eventResizeHandling: "Disabled",
  eventDeleteHandling: "Disabled",
  timeRangeSelectedHandling: "Disabled",
  
  onBeforeCellRender: (args) => {
    if (args.cell.start.toString("yyyy-MM-dd") === singaporeToday) {
      args.cell.properties.backColor = "#fef3c7";
      args.cell.properties.headerHtml =
        `${args.cell.start.getDay()} · Today (SG)`;
    }
  },
  
  onEventClick: (args) => {
    args.preventDefault();
    showTrainingDetails(args.e.data, args.e.data.attendeeNames);
  },

  onBeforeEventRender: (args) => {
    const event = args.data;
    const colours = categoryColours[event.category];
	const confirmed =
	  event.status === "APPROVED" || event.status === "COMPLETED";

	const inactive =
	  ["REJECTED", "CANCELLED", "DELETED"].includes(event.status);

	event.backColor = inactive
	  ? "#e5e7eb"
	  : confirmed ? colours.solid : colours.pale;

	event.fontColor = confirmed ? "#ffffff" : "#111827";
	event.borderColor = inactive ? "#6b7280" : colours.solid;

	const sessionLabel = event.duration === "HALFDAYAM"
	  ? " · AM"
	  : event.duration === "HALFDAYPM"
	    ? " · PM"
	    : "";

	const viewLabel = event.attendeeNames
	  ? ` · ${event.attendeeNames.length} attending`
	  : ` · ${event.status}`;

	const employeeLabel = teamTraining
	    ? `${event.employeeName} · `
	    : "";

	event.text =
	    `${employeeLabel}${event.courseName}${sessionLabel}${viewLabel}`;

	event.toolTip =
		`${employeeLabel}${event.courseName} | ${categoryLabels[event.category]}`
	  + ` | ${durationLabels[event.duration] || "Not specified"}`
	  + ` | ${event.status}`
	  + ` | ${event.approvedParticipants} approved participants`;
  }
});

function showTrainingDetails(event, attendees = null) {

	document.getElementById("details-employee-label").textContent =
	  attendees ? "Approved attendees" : "Employee";

	document.getElementById("details-employee").textContent =
	  attendees ? attendees.join(", ") : event.employeeName;
	  
	document.getElementById("details-course").textContent =
	  event.courseName;

  document.getElementById("details-category").textContent =
    categoryLabels[event.category];

  document.getElementById("details-period").textContent =
    `${event.courseStartDate} to ${event.courseEndDate}`;

  document.getElementById("details-duration").textContent =
    durationLabels[event.duration] || "Not specified";

  document.getElementById("details-status").textContent =
    event.status;

  document.getElementById("details-location").textContent =
    event.location || "Not specified";

  document.getElementById("details-provider").textContent =
    event.trainingProvider || "Not specified";

  document.getElementById("details-participants").textContent =
    event.approvedParticipants;

	const viewLink =
	  document.getElementById("details-view-application");

	const editLink =
	  document.getElementById("details-edit-application");

	const canEdit = !allStaff
	  && !teamTraining
	  && (event.status === "APPLIED" || event.status === "UPDATED");

	viewLink.hidden = allStaff;
	editLink.hidden = !canEdit;

	// Clear links left over from the previously opened application.
	viewLink.removeAttribute("href");
	editLink.removeAttribute("href");

	if (!allStaff) {
	  const viewUrl = new URL(
	    calendarElement.dataset.viewUrl,
	    window.location.origin
	  );

	  viewUrl.searchParams.set("id", event.applicationId);
	  viewLink.href = viewUrl.href;
	}

	if (canEdit) {
	  const editUrl = new URL(
	    calendarElement.dataset.editUrl,
	    window.location.origin
	  );

	  editUrl.searchParams.set("id", event.applicationId);
	  editLink.href = editUrl.href;
	}
	
	const applyLink =
	  document.getElementById("details-apply-course");

	const today = singaporeToday;

	// Offer future approved courses that the logged-in user has not applied for.
	const canApply = (allStaff || teamTraining)
	  && event.status === "APPROVED"
	  && event.courseStartDate > today
	  && !event.alreadyApplied;

	applyLink.hidden = !canApply;
	applyLink.removeAttribute("href");

	if (canApply) {
	  const applyUrl = new URL(
	    calendarElement.dataset.applyUrl,
	    window.location.origin
	  );

	  applyUrl.searchParams.set("courseId", event.courseId);
	  applyLink.href = applyUrl.href;
	}
	
  trainingDetails.showModal();
}

function groupCourses(events, selectedMonth, sharedView) {
  const groups = new Map();

  for (const event of events) {
    // The calendar grid also contains adjacent-month dates.
    if (new DayPilot.Date(event.start).toString("yyyy-MM")
        !== selectedMonth) {
      continue;
    }

    if (sharedView && event.status !== "APPROVED") {
      continue;
    }

    const key = sharedView ? event.courseId : event.applicationId;

    if (!groups.has(key)) {
      groups.set(key, {
        course: event,
        attendees: new Map()
      });
    }

    groups.get(key).attendees.set(event.employeeId, event.employeeName);
  }

  return Array.from(groups.values()).sort((a, b) =>
    a.course.courseStartDate.localeCompare(b.course.courseStartDate)
      || a.course.courseName.localeCompare(b.course.courseName));
}

function addText(parent, tag, text) {
  const element = document.createElement(tag);
  element.textContent = text;
  parent.append(element);
  return element;
}

function renderCourseList(events) {
  courseList.replaceChildren();

  const selectedMonth = new DayPilot.Date(calendar.startDate)
    .toString("yyyy-MM");

  const groups = groupCourses(events, selectedMonth, allStaff);

  if (groups.length === 0) {
    addText(courseList, "p", "No training in the selected month.");
    return;
  }

  for (const [category, label] of Object.entries(categoryLabels)) {
    const categoryGroups = groups.filter(group =>
      group.course.category === category);

    if (categoryGroups.length === 0) {
      continue;
    }

    addText(courseList, "h3", label);

    for (const group of categoryGroups) {
      const course = group.course;

      const card = document.createElement("article");
      card.className = "calendar-course-card";
      card.style.borderLeftColor = categoryColours[category].solid;
      courseList.append(card);

	  const button = addText(
	    card,
	    "button",
	    teamTraining
	      ? `${course.employeeName} · ${course.courseName}`
	      : course.courseName
	  );
      button.type = "button";
      button.className = "calendar-course-title";

      button.addEventListener("click", () => {
        const attendees = allStaff
          ? Array.from(group.attendees.values())
          : null;

        showTrainingDetails(course, attendees);
      });

      addText(card, "p",
        `${course.courseStartDate} to ${course.courseEndDate}`
        + ` · ${durationLabels[course.duration] || "Not specified"}`);

      if (allStaff) {
        addText(card, "p",
          `${course.approvedParticipants} approved participants`);

        const list = document.createElement("ul");
        card.append(list);

        for (const name of group.attendees.values()) {
          addText(list, "li", name);
        }
      } else {
        addText(card, "p", `Status: ${course.status}`);
      }
    }
  }
}

function buildCalendarBars(events, sharedView) {
  const groups = new Map();

  for (const event of events) {
    if (sharedView && event.status !== "APPROVED") {
      continue;
    }

    const key = sharedView ? event.courseId : event.applicationId;

    if (!groups.has(key)) {
      groups.set(key, {
        sample: event,
        dates: new Set(),
        attendees: new Map()
      });
    }

    const group = groups.get(key);

    group.dates.add(
      new DayPilot.Date(event.start).toString("yyyy-MM-dd")
    );

    group.attendees.set(event.employeeId, event.employeeName);
  }

  const bars = [];

  for (const [key, group] of groups) {
    const dates = Array.from(group.dates).sort();
    let currentBar = null;

    for (const date of dates) {
      const end = new DayPilot.Date(date)
        .addDays(1)
        .toString("yyyy-MM-dd");

      if (currentBar && currentBar.end === date) {
        currentBar.end = end;
        continue;
      }

      currentBar = {
        ...group.sample,
        id: `${sharedView ? "course" : "application"}-${key}-${date}`,
        text: group.sample.courseName,
        start: date,
        end,
        attendeeNames: sharedView
          ? Array.from(group.attendees.values())
          : null
      };

      bars.push(currentBar);
    }
  }

  return bars;
}

calendar.init();

const calendarPanel = calendarElement.parentElement;
const courseSidebar = document.getElementById("course-sidebar");

const sidebarResizeObserver = new ResizeObserver(() => {
  courseSidebar.style.height = `${Math.min(
    calendarElement.offsetHeight,
    calendarPanel.clientHeight
  )}px`;
});

sidebarResizeObserver.observe(calendarElement);
sidebarResizeObserver.observe(calendarPanel);

async function loadEvents() {
	
	trainingDetails.close();
	
	courseList.replaceChildren();

	courseListHeading.textContent = teamTraining
	  ? "Team applications this month"
	  : allStaff
	    ? "Approved courses this month"
	    : "My training this month";

	addText(courseList, "p", "Loading courses...");
	
  const requestNumber = ++latestRequest;

  calendar.update({ events: [] });
  calendarMessage.textContent = "Loading training...";

  const url = new URL(
    calendarElement.dataset.eventsUrl,
    window.location.origin
  );

  url.searchParams.set(
    "start",
    calendar.visibleStart().toString("yyyy-MM-dd")
  );

  // Convert DayPilot's exclusive end to our endpoint's inclusive end.
  url.searchParams.set(
    "end",
    calendar.visibleEnd().addDays(-1).toString("yyyy-MM-dd")
  );

  url.searchParams.set("teamTraining", String(teamTraining));
  
  url.searchParams.set("allStaff", String(allStaff));

  if (categorySelect.value !== "") {
    url.searchParams.set("category", categorySelect.value);
  }

  try {
    const response = await fetch(url, {
      credentials: "same-origin"
    });

    if (requestNumber !== latestRequest) {
      return;
    }

    if (response.status === 401) {
      window.location.assign(calendarElement.dataset.loginUrl);
      return;
    }

    if (!response.ok) {
      throw new Error(`Calendar request failed: ${response.status}`);
    }

    const events = await response.json();

    // The user may have changed filters while JSON was being read.
    if (requestNumber !== latestRequest) {
      return;
    }

	renderCourseList(events);
	
	const bars = buildCalendarBars(events, allStaff);
	calendar.update({ events: bars });

    calendarMessage.textContent = events.length === 0
      ? "No matching training in this calendar view."
      : "";
  } catch (error) {
    if (requestNumber !== latestRequest) {
      return;
    }

	courseList.replaceChildren();
    calendar.update({ events: [] });
    calendarMessage.textContent =
      "Could not load training. Please refresh and try again.";
    console.error(error);
  }
}

function selectView(showAllStaff, showTeamTraining = false) {
  allStaff = showAllStaff;
  teamTraining = showTeamTraining;

  myTrainingButton.setAttribute(
    "aria-pressed", String(!allStaff && !teamTraining)
  );
  allStaffButton.setAttribute("aria-pressed", String(allStaff));

  if (teamTrainingButton) {
    teamTrainingButton.setAttribute(
      "aria-pressed", String(teamTraining)
    );
  }

  document.getElementById("calendar-title").textContent =
    teamTraining
      ? "Team Training Calendar"
      : allStaff
        ? "All Staff Training Calendar"
        : "My Training Calendar";

  document.getElementById("calendar-view-description").textContent =
    teamTraining
      ? "All statuses for your direct subordinates. Click a bar to view the employee’s application."
      : allStaff
        ? "Approved training across the organisation. Click a course to see its attendees."
        : "Your applied, updated and approved training. Click a course to view or edit your application.";

  loadEvents();
}

myTrainingButton.addEventListener("click", () => {
  selectView(false);
});

if (teamTrainingButton) {
  teamTrainingButton.addEventListener("click", () => {
    selectView(false, true);
  });
}

allStaffButton.addEventListener("click", () => {
  selectView(true);
});

categorySelect.addEventListener("change", loadEvents);

function showMonth(selectedDate) {
  const date = new DayPilot.Date(selectedDate).firstDayOfMonth();

  calendar.update({ startDate: date });

  document.getElementById("calendar-month-heading").textContent =
    date.toString("MMMM yyyy");

  monthSelect.value = date.toString("MMMM");

  const selectedYear = date.toString("yyyy");

  // Allow navigation beyond the initially supplied year options.
  if (!Array.from(yearSelect.options)
      .some(option => option.value === selectedYear)) {
    yearSelect.add(new Option(selectedYear, selectedYear));
  }

  yearSelect.value = selectedYear;

  loadEvents();
}

document.getElementById("previous-month").addEventListener("click", () => {
  showMonth(new DayPilot.Date(calendar.startDate).addMonths(-1));
});

document.getElementById("next-month").addEventListener("click", () => {
  showMonth(new DayPilot.Date(calendar.startDate).addMonths(1));
});

document.getElementById("today-month").addEventListener("click", () => {
  showMonth(singaporeToday);
});

document.getElementById("calendar-month-form")
  .addEventListener("submit", (event) => {
    event.preventDefault();

    const monthNumber = String(monthSelect.selectedIndex + 1)
      .padStart(2, "0");

    showMonth(`${yearSelect.value}-${monthNumber}-01`);
  });

loadEvents();