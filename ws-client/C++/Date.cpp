#include "Date.h"

Date::Date(void)
{
}

Date::Date(const time_t& _time) {
	unix_time = _time;
}

time_t Date::getUnixDate() {
	return unix_time;
}


Date::~Date(void)
{
}
