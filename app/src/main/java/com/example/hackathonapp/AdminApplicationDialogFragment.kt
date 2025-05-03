package com.example.hackathonapp

import androidx.appcompat.app.AlertDialog
import android.app.Dialog
import androidx.fragment.app.DialogFragment
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class AdminApplicationDialogFragment : DialogFragment() {

    private lateinit var applications: List<MyApplicationsActivity.ApplicationInfo>
    private lateinit var hackathonId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applications = requireArguments().getParcelableArrayList("apps") ?: listOf()
        hackathonId = requireArguments().getString("hackathonId") ?: ""
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireContext())
        val view = layoutInflater.inflate(R.layout.dialog_admin_applications, null)

        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerViewAdminApplications)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = AdminApplicationAdapter(applications, hackathonId)

        builder.setView(view)
            .setTitle("Заявки на хакатон")
            .setNegativeButton("Закрыть", null)

        return builder.create()
    }

    companion object {
        fun newInstance(apps: List<MyApplicationsActivity.ApplicationInfo>, hackathonId: String): AdminApplicationDialogFragment {
            val fragment = AdminApplicationDialogFragment()
            val args = Bundle()
            args.putParcelableArrayList("apps", ArrayList(apps))
            args.putString("hackathonId", hackathonId)
            fragment.arguments = args
            return fragment
        }
    }
}
