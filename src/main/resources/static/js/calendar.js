const calendarElement = document.getElementById("dp");
const calendarMessage = document.getElementById("calendar-message");
const myTrainingButton = document.getElementById("my-training");
const allStaffButton = document.getElementById("all-staff-training");
const categorySelect = document.getElementById("calendar-category");
const monthSelect = document.getElementById("calendar-month");
const yearSelect = document.getElementById("calendar-year");
const trainingDetails = document.getElementById("training-details");

let allStaff = false;
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
  
  onEventClick: (args) => {
    args.preventDefault();
    showTrainingDetails(args.e.data);
  },

  onBeforeEventRender: (args) => {
    const event = args.data;
    const colours = categoryColours[event.category];
    const approved = event.status === "APPROVED";

    event.backColor = approved ? colours.solid : colours.pale;
    event.fontColor = approved ? "#ffffff" : "#111827";
    event.borderColor = colours.solid;

    const duration = event.duration === "HALFDAYAM"
      ? "AM"
      : event.duration === "HALFDAYPM"
        ? "PM"
        : "Full day";

    event.text =
      `${event.text} | ${event.category} | ${duration} | ${event.status}`;

    event.toolTip =
      `${event.courseName} — ${event.approvedParticipants} approved participants`;
  }
});

function showTrainingDetails(event) {
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

  document.getElementById("details-employee").textContent =
    event.employeeName;

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

  trainingDetails.showModal();
}

calendar.init();

async function loadEvents() {
	
	trainingDetails.close();
	
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

    calendar.update({ events });

    calendarMessage.textContent = events.length === 0
      ? "No matching training in this calendar view."
      : "";
  } catch (error) {
    if (requestNumber !== latestRequest) {
      return;
    }

    calendar.update({ events: [] });
    calendarMessage.textContent =
      "Could not load training. Please refresh and try again.";
    console.error(error);
  }
}

function selectView(showAllStaff) {
  allStaff = showAllStaff;

  myTrainingButton.setAttribute("aria-pressed", String(!allStaff));
  allStaffButton.setAttribute("aria-pressed", String(allStaff));

  document.getElementById("calendar-title").textContent = allStaff
    ? "All Staff Training Calendar"
    : "My Training Calendar";

  document.getElementById("calendar-view-description").textContent =
    allStaff
      ? "Approved training for all employees."
      : "Your applied, updated and approved training.";

  loadEvents();
}

myTrainingButton.addEventListener("click", () => {
  selectView(false);
});

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
  showMonth(DayPilot.Date.today());
});

document.getElementById("calendar-month-form")
  .addEventListener("submit", (event) => {
    event.preventDefault();

    const monthNumber = String(monthSelect.selectedIndex + 1)
      .padStart(2, "0");

    showMonth(`${yearSelect.value}-${monthNumber}-01`);
  });

loadEvents();